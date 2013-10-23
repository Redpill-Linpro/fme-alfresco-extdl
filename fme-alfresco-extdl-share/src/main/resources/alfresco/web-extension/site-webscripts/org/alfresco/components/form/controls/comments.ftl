<#include "common/editorparams.inc.ftl" />

<#if field.control.params.rows??><#assign rows=field.control.params.rows><#else><#assign rows=2></#if>
<#if field.control.params.columns??><#assign columns=field.control.params.columns><#else><#assign columns=60></#if>

<div class="form-field">
   <#if form.mode == "view">
      <div class="viewmode-field">
         <#if field.mandatory && field.value == "">
            <span class="incomplete-warning"><img src="${url.context}/res/components/form/images/warning-16.png" title="${msg("form.field.incomplete")}" /><span>
         </#if>
         <span class="viewmode-label">${field.label?html}:</span>
         <#if field.control.params.activateLinks?? && field.control.params.activateLinks == "true">
            <#assign fieldValue=field.value?html?replace("((http|ftp|https):\\/\\/[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?\\^=%&:\\/~\\+#]*[\\w\\-\\@?\\^=%&\\/~\\+#])?)", "<a href=\"$1\" target=\"_blank\">$1</a>", "r")>
         <#else>
            <#assign fieldValue=field.value?html>
         </#if>
         <span class="viewmode-value"><#if fieldValue == "">${msg("form.control.novalue")}<#else>${fieldValue}</#if></span>
      </div>
   <#else>
      <label for="${fieldHtmlId}">${field.label?html}:<#if field.mandatory><span class="mandatory-indicator">${msg("form.required.fields.marker")}</span></#if></label>
      <textarea id="${fieldHtmlId}" name="${field.name}" rows="${rows}" cols="${columns}" tabindex="0"
                <#if field.description??>title="${field.description}"</#if>
                <#if field.control.params.styleClass??>class="${field.control.params.styleClass}"</#if>
                <#if field.control.params.style??>style="${field.control.params.style}"</#if>
                <#if field.disabled && !(field.control.params.forceEditable?? && field.control.params.forceEditable == "true")>disabled="true"</#if>>${field.value?html}</textarea>
   </#if>
<script type="text/javascript">//<![CDATA[
    new Alfresco.CommentsControls("${fieldHtmlId}-list").setOptions({
        height: ${args.editorHeight!180},
        width: ${args.editorWidth!700},
       <#if context.properties.nodeRef??>
           itemNodeRef:"${context.properties.nodeRef?js_string}"
       <#elseif (form.mode == "edit" || form.mode == "view") && args.itemId??>
           itemNodeRef:"${args.itemId?js_string}"
       <#else>
           itemNodeRef:""
       </#if>
    }).setMessages(${messages});//]]>
</script>
   <div id="${fieldHtmlId}-list" class="comment-list" style="display:none;">
       <div class="postlist-infobar">
           <div id="${fieldHtmlId}-list-paginator" class="paginator"></div>
       </div>
       <div class="clear"></div>
       <div id="${fieldHtmlId}-list-comments" class="commentsListTitle"></div>
   </div>
</div>