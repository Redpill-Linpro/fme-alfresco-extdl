<import resource="classpath:/alfresco/extension/templates/webscripts/org/alfresco/slingshot/datalists/deviations-evaluator.lib.js">
<import resource="classpath:alfresco/extension/templates/webscripts/org/alfresco/slingshot/datalists/filters.lib.js">
<import resource="classpath:alfresco/extension/templates/webscripts/org/alfresco/slingshot/datalists/parse-args.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/redpill/comments/comments.lib.js">

const REQUEST_MAX = 1000;

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


// var ESCAPES = [[/&/g, "&amp;"], [/</g, "&lt;"], [/>/g, "&gt;"], [/"/g, "&quot;"]]

function escape(value) {
   return value.replace('"','');
   /*
   var escaped = value;
   for(var item in ESCAPES)
       escaped = escaped.replace(ESCAPES[item][0], ESCAPES[item][1]);
   return escaped;
   */
}

function hasUnreadComment(deviationNode) {
	var comments = getComments(deviationNode);
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

function getNoOfAttachments(deviationNode) {
	var children = deviationNode.childAssocs["rpdl:attachments"];
	
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
   var filter = parsedArgs.filter,
      allNodes = [], node,
      items = [];

   if (filter == null || filter.filterId == "all")
   {
      // Use non-query method
      var parentNode = parsedArgs.listNode;
      if (parentNode != null)
      {
         var pagedResult = parentNode.childFileFolders(true, false, Filters.IGNORED_TYPES, -1, -1, REQUEST_MAX, "dl:itemId", false, null);
         allNodes = pagedResult.page;
      }
   }
   else
   {

     
      var filterParams = Filters.getFilterParams(filter, parsedArgs)
         query = filterParams.query;
      
      // Query the nodes - passing in default sort and result limit parameters
      if (query !== "")
      {
         allNodes = search.query(
         {
            query: query,
            language: filterParams.language,
            page:
            {
               maxItems: (filterParams.limitResults ? parseInt(filterParams.limitResults, 10) : 0)
            },
            sort: filterParams.sort,
            templates: filterParams.templates,
            namespace: (filterParams.namespace ? filterParams.namespace : null)
         });
         
         // Is there a handling officer set?
         // Do second filtering (associations) which cannot be targeted in query
        
         if (allNodes != "undefined"){
   	      if (allNodes.length > 0 && filterParams.handlingOfficer != null)
   	      {
   	    	  var allFilteredNodes = [];
   	    	  for (var i = 0 ; i < allNodes.length; i++)
   	    	  {
   	    		  if(allNodes[i].assocs["rpdl:handlingOfficer"] != null)
   	    		  {
   	    			  var handlingOfficers = allNodes[i].assocs["rpdl:handlingOfficer"];
   	    			  
   	    			  for (var j = 0;j<handlingOfficers.length; j++){
   	    				  if (handlingOfficers[j].name == filterParams.handlingOfficer.name){
   	    					allFilteredNodes.push(allNodes[i]);
   	    				  }else {
   	    					  logger.log("inte match");
   	    				  }
   	    			  }
   	    			  
   	    		  }
   	    	  }
   	    	  allNodes = allFilteredNodes;
   	      }

      }
      
   }
   }

   if (allNodes.length > 0)
   {
      for each (node in allNodes)
      {
         try
         {
            var evaledNode = Evaluator.run(node, fields);
            nodeChildren = node.children;
            extraInfo = null;
            unreadComment = null;
            numberOfAttachments = null;
            if (nodeChildren.length > 0) {
               for each (procNode in nodeChildren) {
                  if (procNode.type.indexOf("deviationProcessorNode", 0) != -1) {
                     evaledNode.extraInfo = procNode;
                     evaledNode.unreadComment = hasUnreadComment(procNode);
                     evaledNode.numberOfAttachments = getNoOfAttachments(procNode); 
                  }
               }
            }
            items.push(evaledNode);
         }
         catch(e) {}
      }
   }

   return (
   {
      fields: fields,
      paging:
      {
         totalRecords: items.length,
         startIndex: 0
      },
      parent:
      {
         node: parsedArgs.listNode,
         userAccess:
         {
            create: parsedArgs.listNode.hasPermission("CreateChildren")
         }
      },
      items: items
   });
}

model.data = getData();