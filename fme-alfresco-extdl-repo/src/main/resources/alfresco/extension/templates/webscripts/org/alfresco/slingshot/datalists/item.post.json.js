<import resource="classpath:/alfresco/extension/templates/webscripts/org/alfresco/slingshot/datalists/deviations-evaluator.lib.js">
<import resource="classpath:alfresco/extension/templates/webscripts/org/alfresco/slingshot/datalists/filters.lib.js">
<import resource="classpath:alfresco/extension/templates/webscripts/org/alfresco/slingshot/datalists/parse-args.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/redpill/comments/comments.lib.js">

/**
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */



function hasUnreadComment(item) {
	var comments = getComments(item);
	var currentUserNodeRef = person.nodeRef;
	for each(var comment in comments){
		if (getCommentData(comment).commentAssigneeNodeRef != undefined && getCommentData(comment).commentAssigneeNodeRef.nodeRef != undefined){
			if (String(getCommentData(comment).commentAssigneeNodeRef.nodeRef.toString()) === String(currentUserNodeRef.toString())){
				if (String(getCommentData(comment).commentReadByAssignee) == "false"){
					return "true";
				}
			}
		}
	}
	return "false";
}

function getNoOfAttachments(item) {
	var children = item.childAssocs["rpdl:attachments"];
	
	if (children == null || children.length == 0){
		return 0;
	}else{
		return children.length;
	}
}


/**
 * Main entry point: Return data list with properties being supplied in POSTed arguments
 *
 * @method getData
 */
function getData()
{
   // Use helper function to get the arguments
   var parsedArgs = ParseArgs.getParsedArgs();
   if (parsedArgs === null)
   {
      return;
   }

   var fields = null;
   // Extract fields (if given)
   if (json.has("fields"))
   {
      // Convert the JSONArray object into a native JavaScript array
      fields = [];
      var jsonFields = json.get("fields"),
         numFields = jsonFields.length();
      
      for (count = 0; count < numFields; count++)
      {
         fields.push(jsonFields.get(count).replaceFirst("_", ":"));
      }
   }

   // Try to find a filter query based on the passed-in arguments
   var node = search.findNode(parsedArgs.nodeRef),
      items = [];

   if (node != null)
   {
      try
      {
         var evaledNode = Evaluator.run(node, fields);
         // RPLP change, add extra info (comments and attachments)
         if (node.type.indexOf("deviationProcessorNode", 0) != -1) {
        	 evaledNode.unreadComment = hasUnreadComment(node);
        	 evaledNode.numberOfAttachments = getNoOfAttachments(node); 
         }

      }
      catch(e) {}
   }

   return (
   {
      fields: fields,
      parent:
      {
         node: parsedArgs.listNode,
         userAccess:
         {
            create: parsedArgs.listNode.hasPermission("CreateChildren")
         }
      },
      item: evaledNode
   });
}

model.data = getData();