#!/bin/sh
# (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Lesser General Public License
# (LGPL) version 2.1 which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/lgpl-2.1.html
#
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# Contributors:
#     Florent Guillaume
#     Guillaume Renard

if [ -z "$1" ]; then
  echo Usage: update.sh ../path/to/the-studio-jar/unpacked >&2
  exit 2
fi
JARDIR=$1
STUDIONAME=nuxeo-routing-default
BUNDLE=$(cd `dirname $0`; pwd)/..

RES=$BUNDLE/src/main/resources

EXT_JSF=$RES/OSGI-INF/extensions.xml
echo 'Extracting JSF contrib to' $BUNDLE
echo '<?xml version="1.0" encoding="UTF-8"?>' > $EXT_JSF
echo '<component name="studio.extensions.nuxeo-routing-default.jsf" version="1.0.0">' >> $EXT_JSF
# extract JSF contrib
DIR_UI_C=$(xmllint --xpath "//extension[@target='org.nuxeo.ecm.directory.ui.DirectoryUIManager']" $JARDIR/OSGI-INF/extensions.xml)
echo "  $DIR_UI_C">> $EXT_JSF
LAYOUT_C=$(xmllint --xpath "//extension[@target='org.nuxeo.ecm.platform.forms.layout.WebLayoutManager' and @point='layouts']" $JARDIR/OSGI-INF/extensions.xml)
echo "  $LAYOUT_C">> $EXT_JSF
echo '</component>' >> $EXT_JSF

# replace studio name
find $RES -name extensions.xml -o -name '*.xsd' | while read f; do
  sed -e "s,/layouts/${STUDIONAME}_layout_template.xhtml,/layouts/layout_default_template.xhtml," \
      -e "s,$STUDIONAME,nuxeo-routing-default,g" \
      -i '~' $f
done

# temporary fix, waiting for NXS-1827 to be done
sed -e 's/<property name=\"width\">300<\/property>/<property name=\"width\">70%<\/property>/g' \
    -i '~' $EXT_JSF

cd $BUNDLE
