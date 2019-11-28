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
        capability "Color Control"
        capability "Color Temperature"
        capability "Configuration"
        capability "Refresh"
        capability "Switch"
        capability "Switch Level"
        capability "ChangeLevel"
        capability "Light"
        capability "ColorMode"

        attribute "colorName", "string"
		attribute "WLED_FX", "int"
		attribute "WLED_Pallet", "int"
		attribute "numSegments", "int"
	}

	// simulator metadata
	simulator {
    
	}
    
	// Preferences
    preferences {
		input "uri", "text", title: "base_url", description: "Base URL of WLED host", required: true, displayDuringSetup: true
        input name: "transitionTime", type: "enum", description: "", title: "Transition time", options: [[500:"500ms"],[1000:"1s"],[1500:"1.5s"],[2000:"2s"],[5000:"5s"]], defaultValue: 1000
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
	
	
	// UI tile definitions
    /*
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
    */
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

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

def parseResp(resp) {
    state = resp.data
    synchronize(resp.data)
}

def parsePostResp(resp){

}

def synchronize(data){
    logDebug "Synchronizing status: ${data}"
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

// Color Names
def setGenericTempName(temp){
    if (!temp) return
    def genericName
    def value = temp.toInteger()
    if (value <= 2000) genericName = "Sodium"
    else if (value <= 2100) genericName = "Starlight"
    else if (value < 2400) genericName = "Sunrise"
    else if (value < 2800) genericName = "Incandescent"
    else if (value < 3300) genericName = "Soft White"
    else if (value < 3500) genericName = "Warm White"
    else if (value < 4150) genericName = "Moonlight"
    else if (value <= 5000) genericName = "Horizon"
    else if (value < 5500) genericName = "Daylight"
    else if (value < 6000) genericName = "Electronic"
    else if (value <= 6500) genericName = "Skylight"
    else if (value < 20000) genericName = "Polar"
    def descriptionText = "${device.getDisplayName()} color is ${genericName}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "colorName", value: genericName ,descriptionText: descriptionText)
}

def setGenericName(hue){
    def colorName
    hue = hue.toInteger()
    if (!hiRezHue) hue = (hue * 3.6)
    switch (hue.toInteger()){
        case 0..15: colorName = "Red"
            break
        case 16..45: colorName = "Orange"
            break
        case 46..75: colorName = "Yellow"
            break
        case 76..105: colorName = "Chartreuse"
            break
        case 106..135: colorName = "Green"
            break
        case 136..165: colorName = "Spring"
            break
        case 166..195: colorName = "Cyan"
            break
        case 196..225: colorName = "Azure"
            break
        case 226..255: colorName = "Blue"
            break
        case 256..285: colorName = "Violet"
            break
        case 286..315: colorName = "Magenta"
            break
        case 316..345: colorName = "Rose"
            break
        case 346..360: colorName = "Red"
            break
    }
    def descriptionText = "${device.getDisplayName()} color is ${colorName}"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "colorName", value: colorName ,descriptionText: descriptionText)
}

// Dimmer function
def setLevel(value) {
    setLevel(value,(transitionTime?.toBigDecimal() ?: 1000) / 1000)
}

def setLevel(value,rate) {
	rate = rate.toBigDecimal()
    def scaledRate = (rate * 10).toInteger()
    
	if(value > 0){
		def isOn = device.currentValue("switch") == "on"
		value = (value.toInteger() * 2.55).toInteger()
		
		msg = "{\"on\": true, \"bri\": ${value}}"
		sendEthernetPost("/json/state", msg)
	} else {
		off()
	}
	
	refresh()
}

def startLevelChange(direction){
}

def stopLevelChange(){
}

// Color Functions
def setColor(value){
    if (value.hue == null || value.saturation == null) return
    def rate = transitionTime?.toInteger() ?: 1000

	// Turn off if level is set to 0/black
	if (value.level == 0) {
		off()
		return
	} else {
		level = value.level * 256
	}
	
	// Convert to RGB from HSV
    rgbValue = hsvToRgb(value.hue, value.saturation, value.level)
	
	// Send to WLED
	setRgbColor(rgbValue)
	setGenericName(value.hue)
}

def setColorTemperature(temp){
	on()
	rgbValue = colorTempToRgb(temp)
	setRgbColor(rgbValue)
	setGenericTempName(temp)
}

def setHue(value){
	def color = [:]
	color.hue = value
	color.level = state.bri/256*100
	color.saturation = 100
	
	setColor(color)
}

def setRgbColor(rgbValue){
	// Turn off any active effects
	setEffect(0,0)
	
	// Send Color
	body = "{\"seg\": [{\"col\": [${rgbValue}]}]}"
	log.debug(body)
	sendEthernetPost("/json/state", body)
	refresh()
}

// Device Functions
def refresh() {
    sendEthernet("/json/state")
}

def sendEthernet(path) {
	if(settings.uri != null){
		def params = [
			uri: "${settings.uri}",
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
	if(settings.uri != null){
		
		def params = [
			uri: "${settings.uri}",
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

// Helper Functions
def logDebug(message){
	if(logEnable) log.debug(message)
}

def hsvToRgb(float hue, float saturation, float value) {
	hue = hue/100
	saturation = saturation/100
	value = value/100
	
    int h = (int)(hue * 6)
    float f = hue * 6 - h
    float p = value * (1 - saturation)
    float q = value * (1 - f * saturation)
    float t = value * (1 - (1 - f) * saturation)

    switch (h) {
      case 0: return rgbToString(value, t, p)
      case 1: return rgbToString(q, value, p)
      case 2: return rgbToString(p, value, t)
      case 3: return rgbToString(p, q, value)
      case 4: return rgbToString(t, p, value)
      case 5: return rgbToString(value, p, q)
      default: log.error "Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value
    }
}

def colorTempToRgb(kelvin){
	temp = kelvin/100
	
	if( temp <= 66 ){ 
        red = 255
        green = temp
        green = 99.4708025861 * Math.log(green) - 161.1195681661
        if( temp <= 19){
            blue = 0
        } else {
            blue = temp-10
            blue = 138.5177312231 * Math.log(blue) - 305.0447927307
        }
    } else {
        red = temp - 60
        red = 329.698727446 * Math.pow(red, -0.1332047592)
        green = temp - 60
        green = 288.1221695283 * Math.pow(green, -0.0755148492 )
        blue = 255
    }
	
	rs = clamp(red,0, 255)
	gs = clamp(green,0,255)
	bs = clamp(blue,0, 255)
	
	return "[" + rs + "," + gs + "," + bs + "]";
}

def rgbToString(float r, float g, float b) {
    String rs = (int)(r * 256)
    String gs = (int)(g * 256)
    String bs = (int)(b * 256)
    return "[" + rs + "," + gs + "," + bs + "]";
}

def clamp( x, min, max ) {
    if(x<min){ return min; }
    if(x>max){ return max; }
    return x;
}

// FastLED FX and Palletes
def setEffect(fx, pal){
	body = "{\"on\":true, \"seg\": [{\"fx\": ${fx},\"pal\": ${pal}}]}"
	sendEthernetPost("/json/state", body)
	refresh()
}
