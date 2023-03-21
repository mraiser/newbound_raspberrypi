# newbound_raspberrypi
A collection of useful Newbound Metabot controls for working with the Raspberry Pi and Pi-compatible computers. This is an app in the [Newbound](https://github.com/mraiser/newbound) appserver ecosystem.

# Dependencies
This project requires an up-to-date working installation of the Newbound software
https://github.com/mraiser/newbound

# Installation
These instructions are for Raspberry Pi OS. Some features will not work on other flavors of Debian Linux.

### Dependencies
    
    sudo apt-get install git libssl-dev pkg-config gcc -y

### Install Rust
    
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh

### Install Newbound

    git clone https://github.com/mraiser/newbound.git Newbound
    cd Newbound
    cargo build --release --features=serde_support

### Install Newbound Raspberry Pi app

    mkdir github
    cd github
    git clone https://github.com/mraiser/newbound_raspberrypi.git raspberry
    cd ../data
    ln -s ../github/raspberry/data/raspberry raspberry
    cd ../runtime
    ln -s ../github/raspberry/runtime/raspberry raspberry
    cd ../

### Startup Newbound

    # Launch Newbound as an app
    # This will automatically open a logged-in browser window
    # Use feature "headless" to suppress browser
    # Use feature "webview" to launch as standalone app instead of browser 
    cargo run --release --features=serde_support

OR

    # Install and launch Newbound as a service
    printf "[Unit]
    Description=Newbound

    [Service]
    User=$USER
    Group=$USER
    WorkingDirectory=/home/$USER/Newbound
    ExecStart=bash -c '/home/$USER/.cargo/bin/cargo run --release --features=serde_support &'
    Type=forking
    RemainAfterExit=yes

    [Install]
    WantedBy=multi-user.target
    " | sudo tee /etc/systemd/system/newbound.service
    
    sudo systemctl daemon-reload
    sudo systemctl enable newbound
    sudo systemctl start newbound

### Activate Raspberry app
When Newbound launches for the first time, it will launch the "Applications" app in a web browser with  inactive apps hidden. Click the toggle switch in "Applications" to show the Raspberry app, select it, then click the "ACTIVATE" button.

### Useful config files 
(after first run)

    # Show HTTP port
    cat config.properties

    # Show admin user password
    cat users/admin.properties

