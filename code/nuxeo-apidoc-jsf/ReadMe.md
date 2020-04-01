## About nuxeo-apidoc-jsf

This module holds additional configuration on top of the nuxeo-apidoc-server modules to:

- introspect Seam components and maybe persist them;
- configure JSF views for persisted distributions as Nuxeo documents in the repository,
  and their display on the JSF UI.

A configuration keep has been kept to hide the Seam components introspection:
 - `org.nuxeo.apidoc.hide.seam.components` (boolean, defaults to false)