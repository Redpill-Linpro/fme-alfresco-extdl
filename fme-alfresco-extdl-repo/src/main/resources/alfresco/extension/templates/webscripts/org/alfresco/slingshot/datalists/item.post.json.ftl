<#import "item.lib.ftl" as itemLib />

<#macro customItemJSON item>
   <#escape x as jsonUtils.encodeJSONString(x)>
      <#assign node = item.node>
      <#assign tags><#list item.tags as tag>"${tag}"<#if tag_has_next>,</#if></#list></#assign>
         "nodeRef": "${node.nodeRef}",        
         <#if item.unreadComment??>
          "unreadComment": "${item.unreadComment}",
        </#if>
        <#if item.numberOfAttachments??>
          "numberOfAttachments": "${item.numberOfAttachments}",
        </#if>
         "createdOn": "${xmldate(node.properties.created)}",
         "createdBy":
         {
            "value": "${item.createdBy.userName}",
            "displayValue": "${item.createdBy.displayName}"
         },
         "modifiedOn": "${xmldate(node.properties.modified)}",
         "modifiedBy":
         {
            "value": "${item.modifiedBy.userName}",
            "displayValue": "${item.modifiedBy.displayName}"
         },
         "actionSet": "${item.actionSet}",
         "tags": <#noescape>[${tags}]</#noescape>,
         "permissions":
         {
            "userAccess":
            {
         <#list item.actionPermissions?keys as actionPerm>
            <#if item.actionPermissions[actionPerm]?is_boolean>
               "${actionPerm?string}": ${item.actionPermissions[actionPerm]?string}<#if actionPerm_has_next>,</#if>
            </#if>
         </#list>
            }
         },
         <#if item.custom??>"custom": <#noescape>${item.custom}</#noescape>,</#if>
         "actionLabels":
         {
      <#if item.actionLabels??>
         <#list item.actionLabels?keys as actionLabel>
            "${actionLabel?string}": "${item.actionLabels[actionLabel]}"<#if actionLabel_has_next>,</#if>
         </#list>
      </#if>
         },
         "itemData":
         {
      <#list item.nodeData?keys as key>
         <#assign itemData = item.nodeData[key]>
            "${key}":
         <#if itemData?is_sequence>
            [
            <#list itemData as data>
               <@renderData data /><#if data_has_next>,</#if>
            </#list>
            ]
         <#else>
            <@itemLib.renderData itemData />
         </#if><#if key_has_next>,</#if>
      </#list>
         }
   </#escape>
</#macro>



<#escape x as jsonUtils.encodeJSONString(x)>
{
   "metadata":
   {
      "parent":
      {
      <#if data.parent??>
         <#assign parentNode = data.parent.node>
         "nodeRef": "${parentNode.nodeRef}",
         "permissions":
         {
            "userAccess":
            {
            <#list data.parent.userAccess?keys as perm>
               <#if data.parent.userAccess[perm]?is_boolean>
               "${perm?string}": ${data.parent.userAccess[perm]?string}<#if perm_has_next>,</#if>
               </#if>
            </#list>
            }
         }
      </#if>
      }
   },
   "item":
   {
      <@customItemJSON data.item />
   }
}
</#escape>