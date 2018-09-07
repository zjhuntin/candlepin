#!/usr/bin/python
import os
from subprocess import call
import urllib2
import libxml2
import shutil
import tarfile
from contextlib import contextmanager
from optparse import OptionParser
from tempfile import mkstemp

BASE_DIR = os.path.dirname(os.path.realpath(__file__))
DEFAULT_VERSION = "2.4.0"

@contextmanager
def open_xml(filename):
    """libxml2 does not handle cleaning up memory automatically. This
    context manager will take care of XML documents."""
    doc = libxml2.parseFile(filename)
    yield doc
    doc.freeDoc()

def download_artemis(version, target_dir):
    filename = "apache-artemis-%s-bin.tar.gz" % (version)
    path_to_file = os.path.join(target_dir, filename)
    url = "https://archive.apache.org/dist/activemq/activemq-artemis/%s/%s" % (version, filename)
    if os.path.exists(path_to_file):
        print "Artemis already downloaded."
        return path_to_file

    try:
        print "Downloading artemis: %s" % (url)
        response = urllib2.urlopen(url, timeout = 5)
        content = response.read()

        with open(path_to_file, 'w') as dl_file:
            dl_file.write(content)

        return path_to_file

    except urllib2.URLError as e:
        print "Unable to download artemis install file: %s" % (url)
        raise e


def extract_artemis(basedir, path_to_file):
    filename = os.path.basename(path_to_file).replace("-bin.tar.gz", "")
    file_path = os.path.join(basedir, filename)

    if not os.path.exists(file_path):
        print "Extracting %s to %s" % (filename, basedir)
        tar = tarfile.open(path_to_file, "r:gz")
        tar.extractall(path=basedir)
        tar.close()
    else:
        print "File already extracted."
    return file_path

def install_artemis(version, install_path="/opt"):
    dl_file_path = download_artemis(version, install_path)
    return extract_artemis(install_path, dl_file_path)

def create_broker(artemis_install_path, broker_root_path, broker_name):
    if not os.path.exists(broker_root_path):
        os.mkdir(broker_root_path)

    broker_path = os.path.join(broker_root_path, broker_name)
    if os.path.exists(broker_path):
        print "Broker already exists, skipping creation."
        return broker_path

    print "Creating artemis broker: %s" % (broker_path)
    cmd = "%s/bin/artemis create --user admin --password admin --allow-anonymous %s" % \
          (artemis_install_path, broker_path)
    print cmd

    ret = call(cmd.split(" "))
    if ret:
        raise RuntimeError("Failed to create broker.")
    return broker_path

# Default broker_data_dir is relative to the broker instance.
def modify_broker_xml(broker_xml_path, broker_data_dir):
    print "Updating broker configuration..."

    # Default the data dir in case it was not set.
    if not broker_data_dir:
        broker_data_dir = "./data"

    with open_xml(broker_xml_path) as doc:
        ctx = doc.xpathNewContext()
        ctx.xpathRegisterNs("activemq", "urn:activemq")
        ctx.xpathRegisterNs("core", "urn:activemq:core")

        # Update the acceptor configuration
        acceptor_nodes = ctx.xpathEval("//activemq:configuration/core:core/core:acceptors/core:acceptor")
        acceptor_nodes[0].setProp("name", "netty")
        acceptor_nodes[0].setContent("tcp://localhost:61617")

        # Update the data store locations
        bindings = ctx.xpathEval("//activemq:configuration/core:core/core:bindings-directory")[0]
        bindings.setContent("%s/bindings" % broker_data_dir)

        journal = ctx.xpathEval("//activemq:configuration/core:core/core:journal-directory")[0]
        journal.setContent("%s/journal" % broker_data_dir)

        large_msg = ctx.xpathEval("//activemq:configuration/core:core/core:large-messages-directory")[0]
        large_msg.setContent("%s/largemsgs" % broker_data_dir)

        paging = ctx.xpathEval("//activemq:configuration/core:core/core:paging-directory")[0]
        paging.setContent("%s/paging" % broker_data_dir)

        doc.saveFile(broker_xml_path)


def update_broker_config(broker_path, candlepin_broker_conf, broker_data_dir):
    # Move the generated conf file if it needs to be referenced later.
    broker_xml = os.path.join(broker_path, "etc/broker.xml")
    old_broker_xml = os.path.join(broker_path, "etc/broker.xml.old")
    if not os.path.exists(old_broker_xml):
        print "Copying broker config file."
        shutil.move(broker_xml, old_broker_xml)
    shutil.copy(candlepin_broker_conf, os.path.join(broker_path, "etc"))

    # Update the Acceptor configuration.
    modify_broker_xml(broker_xml, broker_data_dir)

def update_candlepin_config(cp_conf_path):
    handle, tmp_file_path = mkstemp()
    with os.fdopen(handle,'w') as tmp_file:
        with open(cp_conf_path, "r") as cp_conf:
            for line in cp_conf:
                if not line.startswith("candlepin.audit.hornetq.embedded") and \
                        not line.startswith("candlepin.audit.hornetq.broker_url"):
                    tmp_file.write(line)

            tmp_file.write("candlepin.audit.hornetq.embedded=false\n")
            tmp_file.write("candlepin.audit.hornetq.broker_url=tcp://localhost:61617\n")

    os.remove(cp_conf_path)
    shutil.move(tmp_file_path, cp_conf_path)

def cleanup(version, install_dir, broker_root):
    print "Cleaning up artemis installation."
    if os.path.exists(broker_root):
        print "Removing broker root: %s" % (broker_root)
        shutil.rmtree(broker_root)

    artemis_install_path = os.path.join(install_dir, "apache-artemis-%s" % (version))
    if os.path.exists(artemis_install_path):
        print "Removing artemis installation: %s" % (artemis_install_path)
        shutil.rmtree(artemis_install_path)

def parse_options():
    usage = "usage: %prog"
    parser = OptionParser(usage=usage)
    parser.add_option("--version", action="store", type="string", default=DEFAULT_VERSION,
                      help="the version of artemis to install")
    parser.add_option("--broker-config", action="store", type="string",
                      default="%s/../../src/main/resources/broker.xml" % (BASE_DIR),
                      help="the broker config file to use.")
    parser.add_option("--install-dir", action="store", type="string",
                      default="/opt",
                      help="the path to install the artemis package.")
    parser.add_option("--broker-root", action="store", type="string",
                      default="/var/lib/artemis",
                      help="the root path where the broker should be installed")
    parser.add_option("--broker-name", action="store", type="string",
                      default="candlepin",
                      help="the name of the broker to be installed")
    parser.add_option("--broker-data-dir", action="store", type="string",
                      help="the location to store the broker's data files (defaults to broker's data dir)")
    parser.add_option("--candlepin-conf", action="store", type="string",
                      default="/etc/candlepin/candlepin.conf",
                      help="the candlepin config file that should be updated to point to the broker.")
    parser.add_option("--clean", action="store_true", help="clean current installation")



    return parser.parse_args()

def main():
    (options, args) = parse_options()

    if options.clean:
        cleanup(options.version, options.install_dir, options.broker_root)
        return

    artemis_path = install_artemis(options.version, options.install_dir)
    broker_path = create_broker(artemis_path, options.broker_root, options.broker_name)
    update_broker_config(broker_path, options.broker_config, options.broker_data_dir)
    update_candlepin_config(options.candlepin_conf)


if __name__ == "__main__":
    main()
