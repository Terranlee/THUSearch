<%
    /**
     * An include file that handles the sub navigation of a 
     * pages allowing the override section of a given 'job'. 
     * Include where the sub navigation should be displayed.
     *
     * The following variables must exist prior to this file being included:
     *
     * String theJob - The CrawlJob being manipulated.
     * int jobtab - Which to display as 'selected'.
     *          SUPERCEDED BY SUBMODULES 1 - Filters
     *          2 - Settings
     *          SUPERCEDED BY SUBMODULES 4 - Credentials
     *          5 - Criteria
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
                        <td>
                            <b>Available options:</b>
                        </td>
                        <td class="tab_seperator">

                        </td>
                        <td class="tab<%=jobtab==7?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/refinements/submodules.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==7?"_selected":""%>">Submodules</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==2?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/refinements/configure.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==2?"_selected":""%>">Settings</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab<%=jobtab==5?"_selected":""%>" nowrap>
                            <a href="javascript:doGoto('<%=request.getContextPath()%>/jobs/refinements/criteria.jsp?job=<%=theJob.getUID()%>')" class="tab_text<%=jobtab==5?"_selected":""%>">Criteria</a>
                        </td>
                        <td class="tab_seperator">
                        </td>
                        <td class="tab">
                            <a href="javascript:doSubmit()" class="tab_text">Done with the refinement</a>
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
