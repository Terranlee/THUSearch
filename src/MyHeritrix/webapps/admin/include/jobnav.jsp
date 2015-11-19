<%
    /**
     * An include file that handles the sub navigation of a 'job' page. 
     * Include where the sub navigation should be displayed.
     *
     * The following variables must exist prior to this file being included:
     *
     * String theJob - The CrawlJob being manipulated.
     * int jobtab - Which to display as 'selected'.
     *          0 - Modules
     *          SUPERCEDED BY SUBMODULES 1 - Filters
     *          2 - Settings
     *          3 - Overrides
     *          SUPERCEDED BY SUBMODULES 4 - Credentials
     *          5 - Refinements
     *          SUPERCEDED BY SUBMODULES 6 - URL (Canonicalization)
     *          7 - Submodules 
     *
     * @author Kristinn Sigurdsson
     */
%>
    <table cellspacing="0" cellpadding="0">
        <tr>
            <td bgcolor="#0000FF" height="1">
            </td>
        </tr>
        <tr>
            <td>
                <table cellspacing="0" cellpadding="0">
                    <tr>
                        <td nowrap>
                            <b><%=theJob.isProfile()?"Profile":"Job"%> <%=theJob.getJobName()%>:</b>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <% if(theJob.isRunning()){ %>
                            <td class="tab_inactive" nowrap>
                                <a href="javascript:alert('Can not edit modules on running jobs!')" class="tab_text_inactive">Modules</a>
                            </td>
                        <% } else { %>
                            <td class="tab<%=jobtab==0?"_selected":""%>" nowrap>
                                <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/modules.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==0?"_selected":""%>">Modules</a>
                            </td>
                        <% } %>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==7?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/submodules.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==7?"_selected":""%>">Submodules</a>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==2?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/configure.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==2?"_selected":""%>">Settings</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==3?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/per/overview.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==3?"_selected":""%>">Overrides</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==5?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/refinements/overview.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==5?"_selected":""%>">Refinements</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab">
                            <a href="javascript:doSubmit()" class="tab_text"><%=theJob.isNew()?"Submit job":"Finished"%></a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>
            <td bgcolor="#0000FF" height="1">
            </td>
        </tr>
    </table>
