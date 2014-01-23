<#list set.children as item>
   <#if item.kind != "set">
      <#if (item_index % 4) == 0>
      <div class="yui-g"><div class="yui-g first"><div class="yui-u first"><!-- öppnar 3-->
      <#elseif (item_index % 4) == 2>
      <div class="yui-g"><div class="yui-u first"><!-- öppnar 2-->
      <#else>
      <div class="yui-u"><!-- öppnar 1-->
      </#if>
      <@formLib.renderField field=form.fields[item.id] />
      </div><!-- stänger 1 inte modulo-->
      <#if ((item_index % 4) == 1)>
      </div> <!-- stänger 1 modulo-->
      <#elseif ((item_index % 4) == 3)>
      </div></div><!-- stänger 2-->
      </#if>
   </#if>
</#list>


