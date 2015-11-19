package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.jasper.runtime.*;
import java.net.URLDecoder;

public class login_jsp extends HttpJspBase {


  private static java.util.Vector _jspx_includes;

  static {
    _jspx_includes = new java.util.Vector(1);
    _jspx_includes.add("/include/nocache.jsp");
  }

  public java.util.List getIncludes() {
    return _jspx_includes;
  }

  public void _jspService(HttpServletRequest request, HttpServletResponse response)
        throws java.io.IOException, ServletException {

    JspFactory _jspxFactory = null;
    javax.servlet.jsp.PageContext pageContext = null;
    HttpSession session = null;
    ServletContext application = null;
    ServletConfig config = null;
    JspWriter out = null;
    Object page = this;
    JspWriter _jspx_out = null;


    try {
      _jspxFactory = JspFactory.getDefaultFactory();
      response.setContentType("text/html;charset=ISO-8859-1");
      pageContext = _jspxFactory.getPageContext(this, request, response,
      			"/error.jsp", true, 8192, true);
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;


response.setHeader("Cache-Control", "no-cache"); // HTTP 1.1
response.setHeader("Pragma", "no-cache"); // HTTP 1.0
response.setDateHeader ("Expires", 0); // Prevents caching at the proxy server

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n\n");
  String sMessage = null;
      out.write("\n\n");
      out.write("<html>\n    ");
      out.write("<head>\n        ");
      out.write("<title>Heritrix: Login");
      out.write("</title>\n        ");
      out.write("<link rel=\"stylesheet\" \n            href=\"");
      out.print(request.getContextPath());
      out.write("/css/heritrix.css\">\n    ");
      out.write("</head>\n\n    ");
      out.write("<body onload='document.loginForm.j_username.focus()'>\n        ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\" height=\"100%\">\n            ");
      out.write("<tr>\n                ");
      out.write("<td width=\"155\" height=\"60\" valign=\"top\" nowrap>\n                    ");
      out.write("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\"\n                            width=\"100%\" height=\"100%\">\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td align=\"center\" height=\"40\" valign=\"bottom\">\n                                ");
      out.write("<a border=\"0\" \n                                href=\"");
      out.print(request.getContextPath());
      out.write("/\">");
      out.write("<img border=\"0\" src=\"");
      out.print(request.getContextPath());
      out.write("/images/logo.gif\" width=\"145\">");
      out.write("</a>\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                        ");
      out.write("<tr>\n                            ");
      out.write("<td class=\"subheading\">\n                                Login\n                            ");
      out.write("</td>\n                        ");
      out.write("</tr>\n                    ");
      out.write("</table>\n                ");
      out.write("</td>\n                ");
      out.write("<td width=\"100%\">&nbsp;");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td bgcolor=\"#0000FF\" height=\"1\" colspan=\"2\">\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n            ");
      out.write("<tr>\n                ");
      out.write("<td colspan=\"2\" height=\"100%\" valign=\"top\" class=\"main\">\n                    ");
      out.write("<form method=\"post\" \n                        action='");
      out.print( response.encodeURL("j_security_check") );
      out.write("'\n                            name=\"loginForm\">\n                        ");
      out.write("<input type=\"hidden\" name=\"action\" value=\"login\">\n                        ");
      out.write("<input type=\"hidden\" name=\"redirect\" \n                            value=\"");
      out.print(request.getParameter("back"));
      out.write("\">\n                        ");
      out.write("<table border=\"0\">\n                            ");
 if(sMessage != null ){ 
      out.write("\n                                ");
      out.write("<tr>\n                                    ");
      out.write("<td colspan=\"2\" align=\"left\">\n                                    ");
      out.write("<b>");
      out.write("<font color=red>");
      out.print(sMessage);
      out.write("</font>");
      out.write("</b>\n                                    ");
      out.write("</td>\n                                ");
      out.write("</tr>\n                            ");
}
      out.write("\n                            ");
      out.write("<tr>\n                                ");
      out.write("<td class=\"dataheader\">\n                                    Username:\n                                ");
      out.write("</td>\n                                ");
      out.write("<td> \n                                    ");
      out.write("<input name=\"j_username\">\n                                ");
      out.write("</td>\n                            ");
      out.write("</tr>\n                            ");
      out.write("<tr>\n                                ");
      out.write("<td class=\"dataheader\">\n                                    Password:\n                                ");
      out.write("</td>\n                                ");
      out.write("<td>\n                                    ");
      out.write("<input type=\"password\" name=\"j_password\">\n                                ");
      out.write("</td>\n                            ");
      out.write("</tr>\n                            ");
      out.write("<tr>\n                                ");
      out.write("<td colspan=\"2\" align=\"center\">\n                                    ");
      out.write("<input type=\"submit\" value=\"Login\">\n                                ");
      out.write("</td>\n                            ");
      out.write("</tr>\n                    ");
      out.write("</form>\n                ");
      out.write("</td>\n            ");
      out.write("</tr>\n        ");
      out.write("</table>\n    ");
      out.write("</body>\n");
      out.write("</HTML>\n");
    } catch (Throwable t) {
      out = _jspx_out;
      if (out != null && out.getBufferSize() != 0)
        out.clearBuffer();
      if (pageContext != null) pageContext.handlePageException(t);
    } finally {
      if (_jspxFactory != null) _jspxFactory.releasePageContext(pageContext);
    }
  }
}
