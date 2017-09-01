#!/bin/bash

BASE_URL=''

usage()
{
cat <<EOF
usage: $0 [-e [Environment]]|[-n [Hostname]]

OPTIONS:
   -e      Environment (default is qaslot30)
   -u      Base url (do not use this with the -n option)
   -h      Show this message
   -n      This is used by the go pipeline to trigger integration tests

EOF
}

prepareForEnvironment() {
    local ENVIRONMENT=$1
    case ${ENVIRONMENT} in
        devslot2)
            BASE_URL='https://hd5ddkr02lx.digital.hbc.com:8443'
            ;;
        qaslot2)
            BASE_URL='https://hd5qdkr02lx.digital.hbc.com:8443'
            ;;
        devslot30)
            BASE_URL='https://hd5ddkr30lx.digital.hbc.com:8443'
            ;;
        qaslot30)
            BASE_URL='https://hd5qdkr30lx.digital.hbc.com:8443'
            ;;
        stqa_o5a)
            BASE_URL='https://qa.saksoff5th.com'
            ;;
        stqa_s5a)
            BASE_URL='https://www.qa.saks.com'
            ;;
        *)
            echo "ERROR: no config for environment: $ENVIRONMENT"
            exit 1
    esac
}

while getopts "e:n:b:u:h" OPTION
do
    case ${OPTION} in
    e)
	    prepareForEnvironment $OPTARG
	    ;;
	n)
	    BASE_URL='https://'$OPTARG'.digital.hbc.com:8443'
	    ;;
    h)
	    usage
	    exit 1
	    ;;
	u)
	    BASE_URL=$OPTARG
	    ;;
	*)
	    usage
	    exit 1
	    ;;
    esac
done

BASE_URL=${BASE_URL}'/v1'


echo 'Running integration tests against: '${BASE_URL}
(cd integrationTest; sbt test -DbaseUrl=${BASE_URL})
