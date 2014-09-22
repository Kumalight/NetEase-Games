#-*-coding: UTF-8-*-
#!/usr/bin/python

import MySQLdb
import re, sys, time, os
import urllib

__author__ = 'gzdujintao'

devices        = set()
columns        = ''
brand_synonyms = {}

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

def max(data):

    maxcount = data[0][0]
    maxstr   = data[0][1]
    for x in data:
        if x[0] > maxcount:
            maxcount = x[0]
            maxstr   = x[1]
    return ( maxcount , maxstr)

def merge( data ):

    result = [ "|||".join( [ x[0] for x in data ] ) ]
    for i in range( 1 , len(data[0]) - 1 ):
        col_list = [x[i] for x in data]
        myset = set( col_list )
        result.append( max(map(lambda x: (col_list.count(x), x) if x else (0,''), col_list))[1] )

    result.append( str(data[0][ len(data[0]) - 1 ]) )
    return result

def insert_match_result(one, model, manufacturer, con ):

    crawtime = one[ len(one) - 1 ]
    one = [ MySQLdb.escape_string(x) if x else '' for x in one[:len(one)-1] ]
    one.append( crawtime )
    cursor = con.cursor()

    info = []
    info.extend( [UTF8toGBK(manufacturer), UTF8toGBK(model), one[1] ] )
    info.extend( one[4:] )
    info.append( one[0] )  #urls

    try:

        sql = "insert into device_cache_info values("
        sql += ','.join( map(lambda x:"'" + x + "'", info) ).strip(',')
        update = ' ON DUPLICATE KEY UPDATE '
        update +=  ",".join( map(lambda x,y: x + "='" + y + "'", columns,info) ).strip(",")

        sql += ')' + update
        cursor.execute(sql)
        con.commit()

    except Exception,ex:
        print Exception," insert_exception : ",ex


def init():

    connection = MySQLdb.connect(host="192.168.34.161", user="yangwenfeng", passwd="ywf", db="gdas")
    cursor = connection.cursor()
    cursor.execute("select * from device_brand_synonyms;")

    sym = cursor.fetchall()

    for one in sym:
        one = [GBKtoUTF8(x).lower() for x in one ]
        brand_synonyms[ one[0] ] = one[1].split(',')
        brand_synonyms[ one[0] ].append( one[0] )
        #print one[0], ','.join( brand_synonyms[ one[0] ] )

    file_obj = open("devices_log.txt", 'r')

    for line in file_obj.readlines():
        line_split = line.strip().lower().split("/")

        if line_split.__len__() == 1:
             devices.add( line_split[0] + '\t' + line_split[0] )
        else:
            if line_split[0] == line_split[1]:
                devices.add( line_split[0] + '\t' + line_split[1])
            else:
                devices.add( line_split[0] + '\t'+ line_split[1].replace(line_split[0], "") )


    cursor.close()
    return connection
    print "Initialize End...."

def log_filter( device ):

    print "device = ", device

    model               = line_split[1].lower()
    manufacturer        = line_split[0].lower()

    if 'verizon' in manufacturer and ( 'sch' in model or 'sph' in model or 'sm' in model):
        manufacturer    = 'samsung'

    if 'samsung' == manufacturer or 'motorola' == manufacturer:
        model = line_split[1][ line_split[1].find('-') + 1 : ].lower()
    else:
        model = line_split[1].lower()

    if model.startswith('htc') and manufacturer.startswith('htc'):
        model = model.replace('htc','').replace('one','one ')
        manufacturer = 'htc'

    if "htc" == manufacturer:
        model = model.replace('one','one ').replace('  ',' ')
    if 'incredibles' in model:
        manufacturer = 'htc'
    if model.startswith('lenovo') and manufacturer.startswith("lenovo"):
        model = model.replace('lenovo','')
    if manufacturer == 'lenovo':
        model = model.replace('lnv-','')

    if manufacturer == "lge":
        model = model.replace("lg-", "")

    if manufacturer == "hisense":
        model = model.replace("hs-", "")
    if "xiaomi" == manufacturer:
        model = model.replace("mi", "")
        model = model.replace("hm", "")
        model = model.replace("oneplus", "one plus")

    if "huawei" == manufacturer:
        model = model.replace("mediapad", "mediapad ").replace("x1", "x1 ")

    if "huawei" == manufacturer and '-' in model:
        model = model[ 0 : model.find("-") ]

    if manufacturer.startswith("tcl"):
        model = model.replace("tcl_", "")
    if "desires" in model:
        model = model.replace("desires", "desire s")

    if "desirehd" in model and manufacturer.startswith("htc"):
        model = model.replace("desirehd", "desire hd")
        manufacturer = "htc"

    if "google" == manufacturer:
        model = model.replace("nexus", "nexus ")
        model = model.strip().replace("galaxynexus", "galaxy nexus")

    if "sony" == manufacturer:
        model = model.replace("索尼", "").replace("xperia", "xperia ").replace("  ", " ")

    if "yulong" == manufacturer:
        model = model.replace("coolpad", "")
        manufacturer = "coolpad"

    model = model.replace("_", "")

    return (manufacturer, model)

def getsynonyms( manufacturer ):

    for key in brand_synonyms:
        if manufacturer in brand_synonyms[key]:
            return (brand_synonyms[key],key)
    return ("",False)

if __name__ == '__main__':

    connection = init()

    cursor = connection.cursor()

    cursor.execute("select * from device_raw_info;")

    data = cursor.fetchall()

    cursor.execute("desc device_cache_info;")
    rows = cursor.fetchall()
    columns = [x[0] for x in rows]

    modelandmakermatched = 0
    onlymodelmatched     = 0

    for device in devices:

        found = False
        line_split = device.split('\t')
        if len( line_split ) >= 2:

            (manufacturer, model) = log_filter( device )
            print (manufacturer, model)

            origin_manufacturer = line_split[0]
            origin_model        = line_split[1]

            if not found:
                (syn , key) = getsynonyms(manufacturer)
                if syn and manufacturer in syn:
                    condition = ' or '.join( [ "title like '%"+ x +"%' " for x in syn] )
                    sql = "select * from device_raw_info where ( "+ condition +") and title like '%" + model + "%' order by crawltime desc"
                else:
                    sql = "select * from device_raw_info where  title like '%"+ manufacturer +"%' " \
                      " and title like '%" + model +"%'  order by crawltime desc"

                sql = UTF8toGBK( sql )

                cursor.execute( sql )
                data = cursor.fetchall()
                if len(data) >=1:
                    modelandmakermatched += 1
                    found = True
                    insert_match_result(merge(data), model, key, connection)

                print 'foun1 size = ' , len(data)

            if not found:

                sql = "select * from device_raw_info where model = '"+ model + "'"
                cursor.execute(sql)
                data = cursor.fetchall()
                if len( data) >=1:
                    onlymodelmatched += 1
                    found = True
                    insert_match_result(merge(data), model, manufacturer, connection)
                print 'found2 size = ' , len(data)

    cursor.close()
    connection.close()

    print 'matched = ',(modelandmakermatched + onlymodelmatched),'modelandmakermatched = ',modelandmakermatched, "onlymodelmatched = ",onlymodelmatched




