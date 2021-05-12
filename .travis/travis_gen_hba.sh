#!/usr/bin/env bash
set -euo pipefail

supports_replication () {
    [[ -n "${PG_VERSION}" ]] || return 1
    case "${PG_VERSION}" in
        9.4)
            return 0
            ;;
        9.5)
            return 0
            ;;
        9.6)
            return 0
            ;;
        *.*)
            return 1
            ;;
    esac    
    return 0
}

supports_scram () {
    [[ -n "${PG_VERSION}" ]] || return 1
    case "${PG_VERSION}" in
        *.*)
            return 1
            ;;
    esac    
    return 0
}

main () {
    echo "local   all             all                             trust"
    if supports_replication; then
        echo "host    replication     postgres   0.0.0.0/0        trust"
    fi
    if supports_scram; then
        echo "host    all             test       0.0.0.0/0        scram-sha-256"
        echo "host    all             testscram  0.0.0.0/0        scram-sha-256"
    else
        echo "host    test            all        0.0.0.0/0        md5"
    fi
    echo "host    all             postgres       0.0.0.0/0        trust"
}

main "$@"
