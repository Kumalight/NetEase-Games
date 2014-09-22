#!/usr/bin/python
#-*- coding: utf-8 -*-

import MySQLdb
import re, sys, time


header = ''

def regexsearch(regex, str):
    match = re.search(regex, str)
    if match:
        tmp = match.group()
        return tmp[ tmp.index(':')+1 : ]
    else:
        return ''


def extractInfo(tag , Info, one ):
    m = one[4]
    Info['tag'] = tag
    Info['id']  = one[0]
    Info['game'] =  regexsearch('\[game:[a-zA-Z0-9]+',m)

    Info['username'] = regexsearch('\[username:[a-zA-Z0-9]+',m)
    if not Info['username']:
        Info['username'] = regexsearch('\[user:[a-zA-Z0-9]+',m)
    if not Info['username']:
        Info['username'] = regexsearch('username:[a-zA-Z0-9]+',m)

    Info['dateid']= one[1].strftime("%Y%m%d")
 
    Info['en'] = regexsearch('\[en:[a-zA-Z]+',m)
    Info['field_id'] = regexsearch('\[tbid:[0-9]+',m)
    if not Info['field_id']:
        Info['field_id'] = regexsearch('\[id:[0-9]+',m)


def parseMessage(one):
    m = one[4]
    Info = {'tag':'','id':'', 'game':'',
            'username':'','dateid':'',
            'en':'','field_id':''}

    if not m:
        return Info
    
    #login
    if m.startswith(headers[0]):
        extractInfo(headers[0], Info, one)

    #indicator    
    elif m.startswith( headers[1]):
        extractInfo(headers[1], Info, one)

    #dw
    elif m.startswith( headers[2]):
        extractInfo(headers[2], Info, one)
        
    elif m.startswith( headers[3] ):
        extractInfo(headers[3], Info, one)    

    elif m.startswith( headers[4] ):
        extractInfo(headers[4], Info, one) 
        
    elif m.startswith( headers[5] ):
        extractInfo(headers[5], Info, one)

    #player
    elif m.startswith( headers[6] ):
        extractInfo(headers[6], Info, one)
        
    elif m.startswith( headers[7] ):
        extractInfo(headers[7], Info, one)
        
    elif m.startswith( headers[8] ):
        extractInfo(headers[8], Info, one)
        
    elif m.startswith( headers[9]):
        extractInfo(headers[9], Info, one)
        
    #visualization
    elif m.startswith( headers[10]):
        extractInfo(headers[10], Info, one)
    
    #app    
    elif m.startswith( headers[11] ):
        extractInfo(headers[11], Info, one)

    #report        
    elif m.startswith( headers[12] ):
        extractInfo(headers[12], Info, one)
    elif m.startswith( headers[13] ):
        extractInfo(headers[13], Info, one)
        
    #mobilegame    
    elif m.startswith( headers[14]):
        extractInfo(headers[14], Info, one)
    #default
    else:
        return None
    
    return Info


def convert(value):
    return int(value) if value else 0



if __name__ == '__main__':


   starttime, endtime = ', '

   try:
	starttime = time.strptime(sys.argv[1], "%Y%m%d")
	endtime   = time.strptime(sys.argv[2], "%Y%m%d")

   except:
	print "\t日期格式错误，正确格式如20120202"
	exit()

   con = MySQLdb.connect(host="localhost",user="us_gdas",passwd="Ngvm9KD71XpComYd",db="us_hbase_meta")
   cursor = con.cursor()

   cursor.execute("select * from gdas_log4j where date_format(`datetime`,'%Y%m%d') between "+sys.argv[1] +" and "+\
     sys.argv[2] +" and level = 'INFO'")


   data = cursor.fetchall()
   # print len(data)

   headers = {0:'[login success]', 1:'[indicator]', 2:'[dw search]',
           3:'[dw baseStat]', 4:'[dw groupStat]',5:'[dw search download]',
           6:'[player search]', 7:'[player search download]',
           8:'[player baseStat]',9:'[player groupStat]',
           10:'[visualization]',11:'[app]',12:'[reportView]',
           13:'[report indicator]',14:'[mobile game]'}

      
   result = [] 

   for one in data:

    	dicobj = parseMessage(one)

    	if dicobj:

	   result.append((convert(dicobj['id']), dicobj['tag'], dicobj['game'], dicobj['username'], convert(dicobj['dateid']),dicobj['en'],convert(dicobj['field_id'])))

   sql_delete  = 'delete from gdas_log4j_etl where dateid between '+sys.argv[1]+' and '+sys.argv[2]
    #print sql_delete  
   cursor.execute( sql_delete )
   con.commit()

   sql_insert  = 'insert into gdas_log4j_etl(id, tag, game, username, dateid, en, field_id) values(%s,%s,%s,%s,%s,%s,%s)'
  
   cursor.executemany( sql_insert, tuple(result) )
   con.commit() 

   print '日志处理完毕~'+\
         '\t新插入记录条数：' + str(len(data)) +\
         '\t起始日期：'+sys.argv[1]+' 结束日期：'+sys.argv[2]

