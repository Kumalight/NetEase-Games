<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Portable Device Retrieval</title>
		
	<link rel="stylesheet" type="text/css" href="<%=application.getContextPath() %>/js/jquery-ui-1.9.2.custom/css/custom-theme/jquery-ui-1.9.2.custom.css" />
	<script src="<%=application.getContextPath() %>/js/jquery-ui-1.9.2.custom/js/jquery-1.8.3.js"></script>
	<script src="<%=application.getContextPath() %>/js/jquery-ui-1.9.2.custom/js/jquery-ui-1.9.2.custom.js"></script>


</head>
<body>

	<form id = 'brandandmodel'>
		<span >Query:</span> <input id='query' title='query' value='samsung I9300'></input>
		
	</form>
	<button type = submit value ='Search' id='search'>Search</button>
	
	<br>
	<span style="font-size: 24px;color: #2f7ed8;font-family: Arial,微软雅黑;">查询结果摘要：</span>
	<div id= summary_param></div>
	
	<span style="font-size: 24px;color: #2f7ed8;font-family: Arial,微软雅黑;">查询结果明细：</span>
	<div id= detail_param></div>
	
</body>

<script type="text/javascript">

function render(obj){
	if( null == obj )
		return "<p>无数据</p>";
	var html = '<table border=0px solid #F00 cellspadding = 0>';
	for(var key in obj){
		if( 'headers' != key ){
			
			if (key == 'url'){
				html += "<tr><td><b>" + key + "</b></td><td>";
				var arr = obj[key].split("|||");

				for( var index in arr ){
					html += "<a target=_blank href="+arr[index]+">"+arr[index]+"</a><br>";
				}
				html +="</td></tr>";
				
			}else{
				html += "<tr><td><b>"+ key + "</b></td><td>"+ obj[key] + "</td></tr>";
			}
		}
    }
	
	return html += '</table><hr/>';
}

$(function() {

	$("button#search").click(function(event) {
		
		alert('query='+$("#query").val());
		var serializeArray = $("form").serialize();
		
		alert("serializeArray="+serializeArray);
		$.ajax({
			url: "/USPortableDeviceRetrieval/PdaRetrievalServlet",
			data: {'query':$("#query").val()},
			type: "get",
			async: true,
			dataType: 'json',
			error: function(xhr, message, obj) {
				
		        console.log("ERR:",xhr.responseText, message, obj);	
				alert("计算出错了，请检查参数！");
				return;
			},
			success:function(json){
				
				$("div#summary_param").html(render(json.summary));
				
				var detail_html = '';
				if( null == json.detail || json.detail.length == 0){
					detail_html = "<p>无数据</p>";
				}else{
					for(var index in json.detail)
					{	
						var obj = json.detail[index];
						detail_html += render(obj);
					}
				}

				$("div#detail_param").html(detail_html);

			}
		});
	});
	
});

</script>
</html>