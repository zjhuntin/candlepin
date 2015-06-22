#!/bin/sh

if [ $(id -u) != 0 ]; then
    exec sudo -- "$0" "$@"
fi

unset CDPATH
SCRIPT_NAME=$( basename "$0" )
SCRIPT_HOME=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
IMAGE_NAME=$( basename "$SCRIPT_HOME" )

usage() {
    cat <<HELP
Usage: $SCRIPT_NAME [options] [image name]

OPTIONS:
  -p          Push images to a repository or registry
  -d <repo>   Specify the destination repo to receive the images; implies -p;
              defaults to "candlepin-base docker.usersys.redhat.com/candlepin"
  -c          Use cached layers when building containers
  -v          Enable verbose/debug output
HELP
}

while getopts ":pd:cv" opt; do
    case $opt in
        p  ) PUSH="1";;
        d  ) PUSH="1"
             PUSH_DEST="${OPTARG}";;
        c  ) USE_CACHE="1";;
        v  ) set -x;;
        ?  ) usage; exit;;
    esac
done

shift $(($OPTIND - 1))

if [ "$PUSH_DEST" == "" ]; then
    PUSH_DEST="docker.usersys.redhat.com/candlepin"
fi

# Setup build arguments
BUILD_ARGS=""

if [ "$USE_CACHE" == "1" ]; then
    BUILD_ARGS="$BUILD_ARGS --no-cache=false"
else
    BUILD_ARGS="$BUILD_ARGS --no-cache=true"
fi

# Determine image name
if [ "$1" != "" ]; then
  IMAGE_NAME=$1
fi



cd $SCRIPT_HOME
docker build $BUILD_ARGS -t candlepin/$IMAGE_NAME:latest .

if [ "$?" != "0" ]; then
    exit 1
fi

if [ "$PUSH" == "1" ]; then
    CP_VERSION=`docker run -ti candlepin/$IMAGE_NAME:latest rpm -q --queryformat '%{VERSION}' candlepin`
    if [ "$CP_VERSION" != "" ]; then
        echo "Unable to determine Candlepin version for tagging"
        exit 1
    fi

    docker tag -f candlepin/$IMAGE_NAME:latest $PUSH_DEST/$IMAGE_NAME:latest
    docker tag -f candlepin/$IMAGE_NAME:latest $PUSH_DEST/$IMAGE_NAME:$CP_VERSION

    docker push $PUSH_DEST/$IMAGE_NAME:$CP_VERSION
fi