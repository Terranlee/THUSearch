// Cookie functions from http://www.quirksmode.org/js/cookies.html
// with permission http://www.quirksmode.org/about/copyright.html
function createCookie(name,value,days,path)
{
	if (days)
	{
		var date = new Date();
		date.setTime(date.getTime()+(days*24*60*60*1000));
		var expires = "; expires="+date.toGMTString();
	}
	else var expires = "";
	if (!path) {
		path = "/";
	}
	document.cookie = name+"="+value+expires+"; path="+path;
}

function readCookie(name)
{
	var nameEQ = name + "=";
	var ca = document.cookie.split(';');
	for(var i=0;i < ca.length;i++)
	{
		var c = ca[i];
		while (c.charAt(0)==' ') c = c.substring(1,c.length);
		if (c.indexOf(nameEQ) == 0) return c.substring(nameEQ.length,c.length);
	}
	return null;
}

function eraseCookie(name,path)
{
	createCookie(name,"",-1,path);
}
