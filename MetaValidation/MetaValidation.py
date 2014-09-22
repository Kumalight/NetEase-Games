#-*-coding: UTF-8 -*-
#!/usr/bin/python

__author__ = 'gzdujintao'

import MySQLdb
import sys,time

__datestr__ = time.strftime('%Y-%m-%d',time.localtime(time.time()))

__log__     = open('meta_validation_' + __datestr__ + '.log','a+')

def GBKtoUTF8(value):
    try:
        return value.decode("GBK").encode("UTF-8")
    except:
        return value

def UTF8toGBK(value):
    try:
        return value.decode("UTF-8").encode("GBK")
    except:
        return value

def getConnection(__host = "123.58.173.147", __user="us_gdas", __passwd="Ngvm9KD71XpComYd", __db="information_schema"):

    return MySQLdb.connect(host= __host, user=  __user, passwd = __passwd, db = __db)

def duplicate_tablename():

    print>>__log__, "duplicate_tablename Start..."

    con = getConnection("123.58.173.147", "us_gdas", "Ngvm9KD71XpComYd", "information_schema")

    sql = 'select TABLE_SCHEMA, TABLE_NAME from tables'
    cursor = con.cursor()
    cursor.execute(sql)
    data = cursor.fetchall()
    print>>__log__,"TABLE_NAME|DB_NAME|DB_NAME"
    tablenames = {}
    for one in data:
        if one[1] in tablenames:
            print>>__log__, one[1],'|',one[0],'|',tablenames[one[1]]
        else:
            tablenames[ one[1] ] = one[0]

    con.close()
    print>>__log__, "duplicate_tablename End...\r\n"

def subjectid_consistent_with_gameid():

    print>>__log__, "subjectid_consistent_with_gameid Start..."

    con = getConnection("123.58.173.147", "us_gdas", "Ngvm9KD71XpComYd", "us_hbase_meta")

    file_obj = open("configuration", 'r')
    process_flag = False

    for tableName in file_obj.readlines():

        if tableName.startswith('<begin1>'):
            process_flag  = True
            continue
        if tableName.startswith("#") or len(tableName.strip()) == 0 or not process_flag:
            continue
        if tableName.startswith("<end1>"):
            break


        cols      = tableName.strip().split("\t")
        if len(cols) == 3:
            if cols[0]:
                tableName = cols[0]
            else:
                raise Exception('not foun tableName')
            subjectId = cols[1] if cols[1] else "subjectId"
            gameId    = cols[2] if cols[2] else "gameId"

        elif len(cols) == 2:
            if cols[0]:
                tableName = cols[0]
            else:
                raise Exception('not foun tableName')
            subjectId = cols[1] if cols[1] else "subjectId"
            gameId    = "gameId"

        elif len(cols) == 1:
            if cols[0]:
                tableName = cols[0]
            else:
                raise Exception('not foun tableName')
            subjectId = "subjectId"
            gameId    = "gameId"

        sql = "select "+ subjectId +", "+ gameId +" from "+ tableName +" where "+subjectId+" div 1000 <> "+ gameId +";"
        cursor = con.cursor()
        try:
            cursor.execute(sql)
        except:
            raise Exception('sql 错误，请检查subjectid和gameid是否正确')
        data = cursor.fetchall()
        print>>__log__,"find ", len(data), 'NonConsistents in table:',tableName
        print>>__log__,"subjectId",'|',"gameId",'|',"TableName"
        for one in data:
            print>>__log__,one(0),'|',one(1),'|',tableName

    con.close()
    print>>__log__, "subjectid_consistent_with_gameid End...\r\n"

def dw_table_dateid():

    print>>__log__, "dw table dataid Type Validation Start..."
    con = getConnection("123.58.173.147", "us_gdas", "Ngvm9KD71XpComYd", "information_schema")

    sql = "select TABLE_SCHEMA, TABLE_NAME from tables where TABLE_NAME  like concat('dw_','%')"
    print>>__log__,"Type",'|',"DbName",'|','tableName'
    cursor = con.cursor()
    cursor.execute(sql)
    data = cursor.fetchall()

    for one in data:
        con = getConnection("123.58.173.147", "us_gdas", "Ngvm9KD71XpComYd", one[0])
        cursor = con.cursor()
        sql = "desc "+one[1]
        cursor.execute(sql)
        types = cursor.fetchone()
        if types[1].startswith("varchar"):
            print>>__log__, types[1],'|',one[0],'|',one[1]

    con.close()
    print>>__log__, "dw table dataid Type Validation End...\r\n"

def auth_exception_detection():

    print>>__log__, "auth_exception_detection Start..."

    file_obj = open("configuration", 'r')

    process_flag = False

    game_ignore_dict = {}

    for line in file_obj.readlines():
        if line.startswith('<begin2>'):
            process_flag  = True
            continue

        if line.startswith("#") or len(line.strip()) == 0 or not process_flag:
            continue

        if line.startswith("<end2>"):
            break

        print line
        game_names = line.strip().split('\t')
        if len(game_names) == 1:
            raise Exception('可忽略游戏名出错，出错行：'+line)
            return
        for x in game_names:
            game_ignore_dict[x] = game_names[0]

    print game_ignore_dict

    con = getConnection("123.58.173.147", "us_gdas", "Ngvm9KD71XpComYd", "us_hbase_meta")

    sql =  "select distinct(C.pid), C.usernameCN, C.department, C.num, E.subject_id div 1000, F.value, F.name " \
           "from (" \
           "select A.pid, A.usernameCN, A.department, count(distinct(B.subject_id div 1000)) as num from gdas_user A, gdas_auth B \
            where A.pid = B.pid and (A.department <> '"+UTF8toGBK('用户体验中心')+"' and A.department <> 'GAC') and B.subject_id div 1000 > 1 group by A.pid" \
            ") C inner join " \
           "( gdas_auth E inner join gdas_game F on E.subject_id div 1000 = F.id) \
		    on C.pid = E.pid " \
           "where  C.num >= 2 and E.subject_id div 1000 > 1 order by C.pid ;"

    cursor = con.cursor()
    cursor.execute(sql)
    data = cursor.fetchall()

    pid = -1
    userinfo = ''
    games_with_ignore = set()
    games_without_ignore = set()
    print>>__log__,'pid','|','usernameCN','|','department','|','gameNum','|','gameNameEN','|','gameNameCN'

    for one in data:

        if one[0] != pid:
            pid = one[0]
            if games_with_ignore.__len__() >= 2:
                print>>__log__, userinfo[0],'|',GBKtoUTF8(userinfo[1]),'|',GBKtoUTF8(userinfo[2]),'|',userinfo[3],'|', " ".join(games_with_ignore),'|'," ".join( games_without_ignore)
            userinfo = one
            games_with_ignore.clear()
            games_without_ignore.clear()
            games_without_ignore.add( GBKtoUTF8(one[5]) )
            games_with_ignore.add( one[6] if one[6] not in game_ignore_dict else game_ignore_dict[one[6]] )

        if one[0] == pid:
            games_with_ignore.add( one[6] if one[6] not in game_ignore_dict else game_ignore_dict[one[6]] )
            games_without_ignore.add( GBKtoUTF8(one[5]) )

    con.close()
    print>>__log__,"auth_exception_detection End...\r\n"

if __name__ == '__main__':

    duplicate_tablename()

    subjectid_consistent_with_gameid()

    dw_table_dateid()

    auth_exception_detection()

    print>>__log__, "MetaValidation END"
    __log__.close()