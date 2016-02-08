#!/bin/zsh

# ADLB JENKINS.ZSH
# Run on the Jenkins server
# Installs ADLB; runs its tests

set -eu

echo
echo "maint/jenkins.zsh ..."
echo

MPICH=/tmp/mpich-install
MPICH=$HOME/sfw/mpich-master

if [[ ! -d ${MPICH} ]]
then
  print "MPICH disappeared!"
  print "You must manually run the MPICH Jenkins test to restore MPICH"
  exit 1
fi

rm -rf autom4te.cache
rm -rf /tmp/exm-install/lb

set -x
PATH=$MPICH/bin:$PATH

MPICC=$(which mpicc)

# Diagnostic:
echo MPICC: $MPICC
$MPICC -show
echo

./bootstrap

# Build once with trace logging on to see if it builds
./configure CC=$MPICC --prefix=/tmp/exm-install/lb \
  --enable-log-debug --enable-log-trace --enable-log-trace-mpi
echo
make clean
make

# Now build for tests without logging
./configure CC=$MPICC --prefix=/tmp/exm-install/lb
echo
make clean
make V=1 install

# Diagostics:
# ldd lib/libadlb.so
# make V=1 apps/batcher.x
# ldd apps/batcher.x

SUITE_RESULT=./tests/result_aggregate.xml
rm -fv ${SUITE_RESULT}

rm -fv tests/*.result(.N)

make V=1 -k test_results || true

# Print a message on stderr to avoid putting it in the Jenkins XML
message()
{
  print -u 2 ${*}
}

inspect_results() {
  print "<testsuite name=\"adlb\">"

  for test_script in tests/*.sh(.N)
  do
    local test_path=${test_script%.sh}
    local test_name=$(basename ${test_path})
    local test_result="${test_path}.result"
    local test_tmp="${test_path}.tmp"
    local test_out="${test_path}.out"

    print "    <testcase name=\"${test_name}\">"

    if [[ ! -f "${test_result}" ]]
    then
      # Failure info
      message "Found ERROR in ${test_name}"
      print "        <failure type=\"generic\">"
      if [[ -f "${test_tmp}" ]]
      then
        print "Script output file contents:"
        print "<![CDATA["
        cat ${test_tmp}
        print "]]>"
        print ""
        print ""
      fi

      if [[ -f "${test_out}" ]]
      then
        print "Out file contents:"
        print "<![CDATA["
        cat ${test_out}
        print "]]>"
      fi
      print "        </failure> "
    fi

    print "    </testcase>"
  done

  print "</testsuite>"
}

set +x
inspect_results > ${SUITE_RESULT}

exit 0
