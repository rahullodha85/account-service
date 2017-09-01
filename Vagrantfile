# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure(2) do |config|
  config.vm.box = "hbcd/centos-6.6_1428414292_virtualbox"
  config.vm.box_url = "http://sd1pgo11lx.saksdirect.com:8081/artifactory/repo/centos/6.6/x86_64/1428414292/virtualbox/centos-6.6-x86_64-1428414292.virtualbox.box"
  config.vm.network "private_network", ip: "192.168.44.23"
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "4096"
    vb.cpus = 2
    vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
    vb.customize ["modifyvm", :id, "--natdnsproxy1", "on"]
    vb.customize ["modifyvm", :id, "--nicpromisc3", "allow-all"]
  end
  config.puppet_install.puppet_version = "3.7.4"

  config.librarian_puppet.puppetfile_dir = "puppet"
  config.librarian_puppet.placeholder_filename = ".gitkeep"

  config.vm.provision "puppet" do |puppet|
    puppet.manifests_path = "puppet"
    puppet.manifest_file = "site.pp"
    puppet.module_path = "puppet/modules"
  end
end
