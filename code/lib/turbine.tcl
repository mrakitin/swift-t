
# turbine.tcl
# Main control functions

package provide turbine [ turbine::c::version ]

namespace eval turbine {

    namespace export init finalize

    # Mode is ENGINE, WORKER, or SERVER
    variable mode

    # Counts of engines, servers, workers
    variable n_adlb_servers
    variable n_engines
    variable n_workers

    # ADLB task priority
    variable priority
    variable default_priority

    # How to display string values in the log
    variable log_string_mode

    # User function
    # param e Number of engines
    # param s Number of ADLB servers
    proc init { engines servers } {

        setup_log_string

        variable priority
        variable default_priority
        set default_priority 0
        reset_priority

        # Set up work types
        enum WORK_TYPE { WORK CONTROL }
        global WORK_TYPE
        set types [ array size WORK_TYPE ]
        adlb::init $servers $types
        c::init [ adlb::amserver ] [ adlb::rank ] [ adlb::size ]

        variable n_adlb_servers
        variable n_engines
        variable n_workers
        set n_adlb_servers $servers
        set n_engines $engines
        set n_workers [ expr [ adlb::size ] - $servers - $engines ]

        turbine::init_rng

        variable mode
        if { [ adlb::rank ] < $engines } {
	    set mode ENGINE
        } elseif { [ adlb::amserver ] == 1 } {
            set mode SERVER
        } else {
	    set mode WORKER
        }

        adlb::barrier
        c::normalize

        argv_init
    }

    # Turbine logging contains string values (possibly long)
    # Setting TURBINE_LOG_STRING_MODE truncates these strings
    proc setup_log_string { } {

        global env
        variable log_string_mode

        # Read from environment
        if { [ info exists env(TURBINE_LOG_STRING_MODE) ] } {
            set log_string_mode $env(TURBINE_LOG_STRING_MODE)
        } else {
            set log_string_mode "ON"
        }

        # Check validity - if valid, return without error
        switch $log_string_mode {
            ON  { return }
            OFF { return }
            default {
                if { [ string is integer $log_string_mode ] } {
                    incr log_string_mode -1
                    return
                }
            }
        }

        # Invalid- fall through to error
        error [ join [ "Requires integer or ON or OFF:"
                       "TURBINE_LOG_STRING_MODE=$log_string_mode" ] ]
    }

    proc reset_priority { } {
        variable priority
        variable default_priority
        set priority $default_priority
    }

    proc set_priority { p } {
        variable priority
        set priority $p
    }

    proc debug { msg } {
        c::debug $msg
    }

    proc finalize { } {
        log "turbine finalizing"
        turbine::c::finalize
        adlb::finalize
    }

    # Set engines and servers in the caller's stack frame
    # Used to get tests running, etc.
    proc defaults { } {

        global env
        upvar 1 engines e
        upvar 1 servers s

        if [ info exists env(TURBINE_ENGINES) ] {
            set e $env(TURBINE_ENGINES)
        } else {
            set e ""
        }
        if { [ string length $e ] == 0 } {
            set e 1
        }

        if [ info exists env(ADLB_SERVERS) ] {
            set s $env(ADLB_SERVERS)
        } else {
            set s ""
        }
        if { [ string length $s ] == 0 } {
            set s 1
        }
    }

    # e: A Tcl error object
    proc abort { e } {
        # Error handling
        puts "CAUGHT ERROR:"
        puts $::errorInfo
        puts "CALLING adlb::abort"
        adlb::abort
    }

    proc turbine_workers { } {
        variable n_workers
        return $n_workers
    }

    proc turbine_workers_future { stack output inputs } {
        store_integer $output [ turbine_workers ]
    }

    proc turbine_engines { } {
        variable n_engines
        return $n_engines
    }

    proc turbine_engines_future { stack output inputs } {
        store_integer $output [ turbine_engines ]
    }

    proc adlb_servers { } {
        variable n_adlb_servers
        return $n_adlb_servers
    }

    proc adlb_servers_future { stack output inputs } {
        store_integer $output [ adlb_servers ]
    }
}
