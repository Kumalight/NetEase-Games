#!/usr/bin/python
#-*- coding: utf-8 -*-

import MySQLdb
import re, sys,time,os
import urllib


def convert(value):
    try:
        return value.decode('UTF-8').encode("GBK")
    except:
        return value

def insert_deviceinfo(con, cursor, sitename):

    print 'sitename = ', sitename

    rootdir = "D:/Indigo Workspace/DeviceDetailsCrawler/DeviceDetails__"+sitename+"__byMaker"

    for parent, dirnames, filenames in os.walk(rootdir):

            for filename in filenames:
                    data = []

                    Info = {"Manufacturer":'',"Model":'','CPUManufacturer':'','CPUModel':'',\
                            'CPUClockSpeed':'' , 'CPUCoreNum':'', 'OS':'','OSVersion':'','ROM':'',\
                            'ScreenSize':'','Resolution':'','MainMemory':'','SD':'','url':'','crawltime':'',\
                            'title':'','type':'','Price':''}
                    fullpath =  os.path.join(parent,filename)

                    file_obj = open ( fullpath ,'r')

                    try:
                       for line in file_obj.readlines():

                         line_split = line.strip().split(':')
                         if len( line_split ) >= 2:

                                item =  line_split[0]
                                value = MySQLdb.escape_string(convert( line_split[1] )) if line_split[1] != '-' else ''
                                if item in ['品牌' , 'Brand'] :
                                      Info['Manufacturer'] = value
                                elif item in [ '型号', '型号别称']:
                                      Info['Model'] = value
                                elif item in ['分辨率', '屏幕分辨率', 'Display Resolution', '主屏分辨率']:
                                      Info['Resolution'] = value
                                elif item in['CPU品牌', '处理器']:
                                      Info['CPUManufacturer'] = value
                                elif item in ['操作系统', '系统', 'Embedded Operating System']:
                                      Info['OS'] = value
                                elif item in ['操作系统版本']:
                                      Info['OSVersion'] = value
                                elif item in ['运行内存','系统内存','运行内存(RAM)','RAM capacity', 'RAM容量'] :
                                      Info['MainMemory'] = value
                                elif item  in ['屏幕尺寸', '主屏尺寸', 'Display Diagonal']:
                                      Info['ScreenSize'] = value
                                elif item  in ['储存卡类型', '可扩展容量', '储存卡', '存储扩展','存储卡', 'Expansion Interfaces']:
                                      Info['SD'] = value
                                elif item in ['机身内存', '存储容量', '内置容量', '机身存储(ROM)', 'ROM capacity in bytes', 'ROM capacity', 'ROM容量'] :
                                      Info['ROM'] = value
                                elif item in [ 'CPU核数', '核心数量', '核心数', '处理器类型',  'CPU核心数', 'CPU Core', '处理器核心']:
                                      Info['CPUCoreNum'] = value
                                elif item in [ 'CPU频率', '处理器速度', '处理器主频', '主频', 'CPU Clock']:
                                      Info['CPUClockSpeed'] = value
                                elif item in ['CPU型号', '处理器', 'CPU', '处理器型号']:
                                      Info['CPUModel'] = value
                                elif item in ['产品价格','Price']:
                                      Info['Price'] = value
                                elif item == '标题':
                                      Info['title'] = value
                                elif item == '时间':
                                      Info['crawltime'] = convert((':').join(line_split[1:]))
                                elif item == '设备类型':
                                      Info['type'] = value

                    finally:
                      file_obj.close()

                    Info['url']= urllib.unquote( filename[0 : len(filename)-4] )

                    data.extend([ Info['url'], Info['title'],Info['Manufacturer'] ,Info['Model'] ,Info['CPUManufacturer'], Info['CPUModel'], \
                                 Info['CPUClockSpeed'],Info['CPUCoreNum'],Info['OS'], Info['OSVersion'], Info['ScreenSize'], Info['Resolution'],\
                                 Info['MainMemory'] ,Info['ROM'], Info['SD'], Info['type'], Info['Price'], Info['crawltime'] ])

                    update = ' ON DUPLICATE KEY UPDATE '
                    count = 0
                    update += ','.join( map(lambda key : key+"='"+Info[key]+"'", Info.keys() ) )

                    for key in Info:
                        if Info[key]:
                            count += 1
                    if count <= 4:
                        continue

                    sql = "insert into device_raw_info values("

                    sql += ','.join( map(lambda x : "'" + x +"'", data) ) + ')'
                    sql += update

                    cursor.execute( sql )
                    con.commit()
    print 'end'

if __name__ == '__main__':

    con = MySQLdb.connect(host="192.168.34.161",user="yangwenfeng",passwd="ywf",db="gdas")
    cursor = con.cursor()

    sitenames = ['PConline' ,'Zol', 'PCpop', 'JD', 'PDAdb']
    for sn in sitenames:
        insert_deviceinfo(con, cursor, sn)