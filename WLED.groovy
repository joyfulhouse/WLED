/**
*  Copyright 2020 Hubitat
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License. You may obtain a copy of the License at:
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
*  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
*  for the specific language governing permissions and limitations under the License.
*
*  WLED Device Type
*
*  Author: bryan.li@gmail.com
*
*  Date: 2019-11-27
*/
import java.net.URLEncoder

metadata {
	definition (name: "WLED", namespace: "joyfulhouse", author: "Bryan Li") {
		capability "Switch"
	}

	// simulator metadata
	simulator {
    
	}
    
	// Preferences
    preferences {
        input "ip", "text", title: "IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true
        input "port", "text", title: "Port", description: "port in form of 80", required: true, displayDuringSetup: true
    }
	
	// UI tile definitions
	tiles(scale: 2) {
        standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "off", label: '${currentValue}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
            state "on", label: '${currentValue}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc"
        }
		
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"polling.poll", icon:"st.secondary.refresh"
		}

		main "switch"
		details(["switch","refresh"])
	}
}

def initialize() {
 	installed()
}
def installed() {
    unschedule()
	refresh()
}

def updated() {
    unschedule()
    refresh()
}

def parseResp(resp) {
    state = resp.data
    synchronize(resp.data)
}

def parsePostResp(resp){

}

def synchronize(data){
    log.debug "Synchronizing status: ${data}"
    // Power
	if(data.on){
		if(device.currentValue("switch") != "on")
			sendEvent(name: "switch", value: "on")
    }
    else {
		if(device.currentValue("switch") == "on")
			sendEvent(name: "switch", value: "off")
    }
}

// Switch Capabilities
def on() {
    sendEthernetPost("/json/state","{\"on\": true}")    
	sendEvent(name: "switch", value: "on")
}

def off() {
	sendEthernetPost("/json/state","{\"on\": false}")    
	sendEvent(name: "switch", value: "off")
}

// Device Functions
def refresh() {
    sendEthernet("/json/state")
}

def sendEthernet(path) {
    if(settings.ip != null && settings.port != null){
        def params = [
            uri: "http://${settings.ip}:${settings.port}",
            path: "${path}",
            headers: [:]
        ]

        try {
            httpGet(params) { resp ->
                parseResp(resp)
            }
        } catch (e) {
            log.error "something went wrong: $e"
        }
    }
}

def sendEthernetPost(path, body) {
    if(settings.ip != null && settings.port != null){
        
        def params = [
            uri: "http://${settings.ip}:${settings.port}",
            path: "${path}",
            headers: [
                    "Content-Type": "application/json",
                    "Accept": "*/*"
                ],
            body: "${body}"
        ]

        try {
            httpPost(params) { resp ->
                parsePostResp(resp)
            }
        } catch (e) {
            log.error "something went wrong: $e"
        }
    }
}
