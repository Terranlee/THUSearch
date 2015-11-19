<%--This page is included by filters.jsp and by url-canonicalization-rules.jsp
    at least.
 --%>
<%@ page import="org.archive.crawler.admin.CrawlJob" %>
<script type="text/javascript">
    function doSubmit(){
        document.frmFilters.submit();
    }
    
    function doGoto(where){
        document.frmFilters.action.value="goto";
        document.frmFilters.subaction.value = where;
        doSubmit();
    }
    
    function doMoveUp(filter,map){
        document.frmFilters.action.value = "filters";
        document.frmFilters.subaction.value = "moveup";
        document.frmFilters.map.value = map;
        document.frmFilters.filter.value = filter;
        doSubmit();
    }

    function doMoveDown(filter,map){
        document.frmFilters.action.value = "filters";
        document.frmFilters.subaction.value = "movedown";
        document.frmFilters.map.value = map;
        document.frmFilters.filter.value = filter;
        doSubmit();
    }

    function doRemove(filter,map){
        document.frmFilters.action.value = "filters";
        document.frmFilters.subaction.value = "remove";
        document.frmFilters.map.value = map;
        document.frmFilters.filter.value = filter;
        doSubmit();
    }

    function doAdd(map){
        if(document.getElementById(map+".name").value == ""){
            alert("Must enter a unique name for the subcomponent");
        } else {
            document.frmFilters.action.value = "filters";
            document.frmFilters.subaction.value = "add";
            document.frmFilters.map.value = map;
            doSubmit();
        }
    }
</script>
