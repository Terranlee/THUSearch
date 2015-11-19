package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import org.apache.jasper.runtime.*;
import org.archive.crawler.admin.CrawlJobHandler;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.Heritrix;
import org.archive.crawler.framework.CrawlController;
import org.archive.crawler.admin.CrawlJob;
import org.archive.crawler.admin.StatisticsTracker;
import java.util.*;
import org.archive.crawler.settings.*;
import java.io.File;

public class vieworder_jsp extends HttpJspBase {


  private static java.util.Vector _jspx_includes;

  static {
    _jspx_includes = new java.util.Vector(1);
    _jspx_includes.add("/include/handler.jsp");
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
      			null, true, 8192, true);
      application = pageContext.getServletContext();
      config = pageContext.getServletConfig();
      session = pageContext.getSession();
      out = pageContext.getOut();
      _jspx_out = out;

      out.write("\n");
      out.write("\n");
      out.write("\n");
      out.write("\n");

    /**
     * This include page ensures that the handler exists and is ready to be
     * accessed.
     */
    CrawlJobHandler handler =
        (CrawlJobHandler)application.getAttribute("handler");
    Heritrix heritrix = (Heritrix)application.getAttribute("heritrix");
    
    // If handler is empty then this is the first time this bit of code is
    // being run since the server came online. In that case get or create the
    // handler.
    if (handler == null) {
        if(Heritrix.isSingleInstance()) {
            heritrix = Heritrix.getSingleInstance();
            handler = heritrix.getJobHandler();
            application.setAttribute("heritrix", heritrix);
            application.setAttribute("handler", handler);
        } else {
            // TODO:
            // If we get here, then there are multiple heritrix instances
            // and we have to put up a screen allowing the user choose between.
            // Otherwise, there is no Heritrix instance.  Thats a problem.
            throw new RuntimeException("No heritrix instance (or multiple " +
                    "to choose from and we haven't implemented this yet)");
        }
    }
    
    // ensure controller's settingsHandler is always thread-installed 
    // in web ui threads
    if(handler != null) {
        CrawlJob job = handler.getCurrentJob();
        if(job != null) {
            CrawlController controller = job.getController();
            if (controller != null) {
                controller.installThreadContextSettingsHandler();
            }
        }
    }

      out.write("\n");
      out.write("\n\n");
      out.write("\n");
      out.write("\n\n\n");

 
    String job = request.getParameter("job");
    CrawlJob cjob = null;

    StatisticsTracker stats = null;
    
    if(job != null)
    {
        cjob = handler.getJob(job);
        stats = (StatisticsTracker)cjob.getStatisticsTracking();
        
    }
    else
    {
        // Assume current job.
        cjob = handler.getCurrentJob();
        stats = (StatisticsTracker)cjob.getStatisticsTracking();
    }
    
    

      out.write("\n");
      out.write("<html>\n    ");
      out.write("<head>\n        ");
      out.write("<title>Heritrix: View crawl order");
      out.write("</title>\n        ");
      out.write("<link rel=\"stylesheet\" href=\"");
      out.print(request.getContextPath());
      out.write("/css/heritrix.css\">\n    ");
      out.write("</head>\n    \n    ");
      out.write("<body>\n        ");

            if(cjob == null)
            {
                // NO JOB SELECTED - ERROR
        
      out.write("\n                ");
      out.write("<b>Invalid job selected");
      out.write("</b>\n        ");

            }
            else
            {
        
      out.write("\n            ");
      out.write("<iframe name=\"frmStatus\" src=\"");
      out.print(request.getContextPath());
      out.write("/iframes/xml.jsp?file=");
      out.print(((XMLSettingsHandler)cjob.getSettingsHandler()).getOrderFile().getAbsolutePath());
      out.write("\" width=\"100%\" height=\"100%\" frameborder=\"0\" >");
      out.write("</iframe>\n        ");

            } // End if(cjob==null)else clause
        
      out.write("\n    ");
      out.write("</body>\n");
      out.write("</html>\n");
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
