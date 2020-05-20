<@extends src="base.ftl">
<@block name="title">Seam Component ${nxItem.name}</@block>

<@block name="right">
<#include "/docMacros.ftl">

<h1>Seam Component <span class="componentTitle">${nxItem.name}</span></h1>

<div class="tabscontent">

  <h2>Scope</h2>
  ${nxItem.scope}

  <h2>Implementation</h2>
  <div class="implementation">
    <@javadoc nxItem.className false />
  </div>

  <#assign hasInterface=false/>
  <#list nxItem.interfaceNames as iface>
    <#if iface != nxItem.className>
      <#assign hasInterface=true/>
      <#break>
    </#if>
  </#list>

  <#if hasInterface>
  <h2>Interfaces</h2>
  <ul class="interfaces">
    <#list nxItem.interfaceNames as iface>
    <#if iface != nxItem.className>
    <li>
      <@javadoc iface false />
    </li>
    </#if>
    </#list>
  </ul>
  </#if>

</div>

</@block>
</@extends>
