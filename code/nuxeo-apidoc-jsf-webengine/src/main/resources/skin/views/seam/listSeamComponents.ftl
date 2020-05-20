<@extends src="base.ftl">
<@block name="title">All Seam Components</@block>
<@block name="header_scripts">
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter.js"></script>
  <script type="text/javascript" src="${skinPath}/script/jquery.tablesorter_filter.js"></script>
</@block>

<#if Root.isEmbeddedMode()>
  <#assign hideNav=true/>
</#if>

<@block name="right">
<#include "/docMacros.ftl">

<h1>${seamComponents?size} Seam Components</h1>

<div class="tabscontent">
  <@tableFilterArea "seam component"/>
  <table id="contentTable" class="tablesorter">
    <thead>
    <tr>
      <th>Scope</th>
      <th>Seam Component</th>
      <th>Class</th>
    </tr>
    </thead>
  <tbody>
  <#list seamComponents as component>
  <#assign rowCss = (component_index % 2 == 0)?string("even","odd")/>
    <tr class="${rowCss}">
      <td><span class="sticker scope">${component.scope}</span></td>
      <td>
        <a href="${Root.path}/${distId}/seam/viewSeamComponent/${component.id}" class="itemLink">${component.name}</a>
      </td>
      <td>
        <@javadoc component.className false />
        <#list component.interfaceNames as iface>
          <#if iface != component.className>
            <br/>
            <@javadoc iface false />
          </#if>
        </#list>
      </td>
    </tr>
  </#list>
  </tbody>
  </table>
</div>

</@block>

<@block name="footer_scripts">
<@tableSortFilterScript "#contentTable" "[1,0]" />
</@block>

</@extends>
