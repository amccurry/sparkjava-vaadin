#!/bin/bash
set -e

OLD_VERSION=$1
NEW_VERSION=$2

PROJECT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

if [[ -z "$OLD_VERSION" || -z "$NEW_VERSION" ]] ; then
    echo "Old and New version need to be specified"
    exit 1
fi

function update_pom(){
  FILE="${1}"
  echo "Updating: ${FILE}"
  xmlstarlet ed --ps -N pom="http://maven.apache.org/POM/4.0.0" \
      -u "/pom:project/pom:version" -v $NEW_VERSION \
      -u "/pom:project/pom:parent/pom:version" -v $NEW_VERSION \
      ${FILE} > ${FILE}.new
  mv ${FILE}.new ${FILE}
}

update_pom ${PROJECT_DIR}/pom.xml
for f in ${PROJECT_DIR}/*/pom.xml ; do
 update_pom $f
done
