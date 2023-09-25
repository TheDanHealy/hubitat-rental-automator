/* Copyright 2023 Dan Healy (thedanhealy.com)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Version History

1.0.0     Initial Release
1.0.1     Update documentation to mention what to do next with the configured modes

*/

definition(
    name: "AirBNB Automator",
    namespace: "thedanhealy-airbnb",
    author: "TheDanHealy",
    description: "Automation for AirBNB",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: ""
)

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.regex.Pattern
import java.util.regex.Matcher
import groovy.json.JsonSlurper



preferences {
    page(name: "mainPage", install: true, uninstall: true) {
        section {
            paragraph """<h1>AirBNB Automator by <a href="https://thedanhealy.com" target="_blank"">Dan Healy</h1></a>"""
            paragraph """If you enjoy using this app, please consider donating to the <a href="https://brotherson3.org" target="_blank"><strong>Josh Minton Foundation</strong></a>. I created this app to help our foundation better manage the AirBNB guests, which provides us with extra funding inbetween our no-cost therapeutic stays. I am now offering this app to the community for free, but in hopes that you'll also donate back in appreciation for the free usage."""
        }
        section {
            if(state.enabled) {
                input name: "disableButton", type: "button", title: "Disable AirBNB Automation"
                paragraph "<p style=\"color:green;\">The AirBNB automation is currently enabled</p>"
            }
            if(!state.enabled) {
                input name: "enableButton", type: "button", title: "Enable AirBNB Automation"
                paragraph "<p style=\"color:red;\"><strong>The AirBNB automation is currently disabled</strong></p>"
            }
        }
        section {
            paragraph "<h2>AirBNB Calendar Settings</h2><hr>"
            input name: "calendarUrl", type: "text", title: "AirBNB Calendar URL", required: true
            paragraph """To learn how to obtain your AirBNB Calendar URL, click <a href="https://www.airbnb.com/help/article/99#section-heading-9-0" target="_blank">here</a>."""
            input name: "testCalendarUrl", type: "button", title: "Test"
            if(state.testCalendarUrlState) {
                paragraph "<p style=\"color:green;\">AirBNB Calendar URL Verified & Tested</p>"
            } 
            if(!state.testCalendarUrlState && state.testCalendarUrlState != null) {
                paragraph "<p style=\"color:red;\"><strong>There's an issue with the AirBNB Calendar URL. Please check the URL and Hubitat logs and re-try</strong></p>"
            }
        }
        section{
            paragraph "<h2>Check-In & Check-Out Settings</h2><hr>"
            input name: "checkinTime", type: "time", title: "When is Check-In Time?", required: true 
            input name: "checkoutTime", type: "time", title: "When is Check-Out Time?", required: true
            input name: "checkinPrep", type: "bool", title: "Do you need to run any preparations before Check-In (called \"Check-In Prep\"), such as cooling or heating?", submitOnChange: true
            if(checkinPrep) {
                input name: "checkinPrepMinutes", type: "num", title: "How many minutes before Check-In should the preparations start?", required: true
            }
            for (lock in doorLocks) {
                if (!lock.hasCommand("setCode")) paragraph "<p style=\"color:red; padding-top: 30px;\"><strong>${lock} DOES NOT SUPPORT PROGRAMMING OF CODES THROUGH HUBITAT</strong></p>"
            }
            input "doorLocks", "capability.lock", title: "Which door lock(s) do you want to program", submitOnChange: true, required: true, multiple: true
            paragraph "<em>If you want to have the door lock codes programmed earlier than Check-In time, please enable the Check-In Prep above.</em>"
            input "checkinMode", "mode", title: "Which mode do you want to activate for Check-In?", submitOnChange: true, required: true 
            if(debugMode) input name: "testCheckinMode", type: "button", title: "Debug: Test activating Check-In"
            if(checkinPrep) {
                input name: "checkinPrepSameMode", type: "bool", title: "Do you want to use the same mode for Check-In Prep as Check-In?", submitOnChange: true
                if(!checkinPrepSameMode) {
                   input "checkinPrepMode", "mode", title: "Which mode do you want to activate for Check-In Prep?", submitOnChange: true, required: true
                   if(debugMode) input name: "testCheckinPrepMode", type: "button", title: "Debug: Test activating Check-In Prep"
                   input name: "programLocksAtCheckinPrep", type: "bool", title: "Do you want to program the door lock codes at Check-In Prep time?"
                }
            }
            input "checkoutMode", "mode", title: "Which mode do you want to activate for Check-Out?", submitOnChange: true, required: true
            if(debugMode) input name: "testCheckoutMode", type: "button", title: "Debug: Test activating Check-Out"
        }
        section {
            paragraph "<h2>Notification Settings</h2><hr>"
            input "notificationDevices", "capability.notification", title: "Which devices do you want to use for push notifications?", multiple: true, required: false, submitOnChange: true
            if(debugMode) input name: "sendTestNotification", type: "button", title: "Debug: Send Test Notification"
            input name: "notificationOnErrorsOnly", type: "bool", title: "Do you want to only be notified when there's error? Otherwise, a notification will be sent when each mode gets activated", submitOnChange: true
        }
        section{
            paragraph "<h2>Save Settings</h2><hr>"
            input name: "saveButton", type: "button", title: "Save"
            input name: "debugMode", type: "bool", title: "Enable Debug Mode", submitOnChange: true
            if(debugMode) {
                input name: "forceEventOverride", type: "bool", title: "Do you want to force the test procedures below to execute on the first calendar event?", submitOnChange: true
                input name: "testCheckinPrepProcedure", type: "button", title: "Test Check-In Prep Procedure"
                input name: "testCheckinProcedure", type: "button", title: "Test Check-In Procedure"
                input name: "testCheckoutProcedure", type: "button", title: "Test Check-Out Procedure"
                input name: "testDoorLockProgramming", type: "button", title: "Test Door Lock Programming"
            }
        }
    }
}

// Called when app first installed
def installed() {
  // for now, just write entry to "Logs" when it happens:
    log.trace "installed()"
}

// Called when user presses "Done" button in app
def updated() {
    state.remove("testCalendarUrlState")
    log.trace "updated()"
}

// Called when app uninstalled
def uninstalled() {
   log.trace "uninstalled()"
   // Most apps would not need to do anything here
}

def enableAutomation() {
    schedule((convertTimeToCron(timeVar = checkinTime)), checkinProcedure)
    schedule((convertTimeToCron(timeVar = checkoutTime)), checkoutProcedure)
    if(checkinPrep) {
        schedule((convertTimeToCron(timeVar = checkinTime, minutesToSubtract = checkinPrepMinutes)), checkinPrepProcedure)
    }
}

def disableAutomation() {
    unschedule(mymethod)
}

def convertTimeToCron(timeVar, minutesToSubtract = 0) {
    if(debugMode) log.debug "The timeVar is ${timeVar}"
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    Calendar calendar = new GregorianCalendar();
    Date timeVarObj = format.parse(timeVar)
    calendar.setTime(timeVarObj)
    calendar.add(Calendar.MINUTE, (-1 * minutesToSubtract.toInteger()) )
    timeVarObj = calendar.getTime()
    if(debugMode) log.debug "The timeVarObj is " + timeVarObj
    String hour = calendar.get(Calendar.HOUR)
    String minute = calendar.get(Calendar.MINUTE)
    String cronExp = "0 ${minute} ${hour} * * ?"
    return cronExp
}

def checkinProcedure(forceEventOverride = false) {
    try {
        if(debugMode) log.debug "Executing the checkinProcedure"
        def iCalData = getCalendarData(calendarUrl)
        def iCalDict = iCalToMapListAirBnB(iCalData)
        def checkinTodayData = checkinToday(iCalDict, forceEventOverride)
        if(checkinTodayData) {
            log.info "Running the AirBNB Check-In Procedure"
            if(debugMode) log.debug "The data for today's Check-In is ${checkinTodayData}"
            location.setMode(checkinMode)
            int retries = 0
            while(retries<3) {
                if(!programLocksAtCheckinPrep) {
                    doorLocks.each { lock ->
                        def nextAvailableCodePosition = findNextAvailableCodePosition(lock)
                        if(debugMode) log.debug "The next available code position returned is ${findNextAvailableCodePosition}"
                        if(nextAvailableCodePosition) {
                            if(debugMode) log.debug "Programming the door lock code now"
                            programDoorLockCode(lock, nextAvailableCodePosition.toInteger(), checkinTodayData.toString())
                            def checkIfCodeExists = findExistingAirbnbCodePosition(lock)
                            if(checkIfCodeExists) {
                                return
                            } else {
                                retries = retries + 1
                            }
                        }
                    }
                }
            }
            log.info "Successfully ran the Check-In procedure and changed the mode to ${checkinMode}"
            if(!notificationOnErrorsOnly) {
                sendNotification "Successfully ran the Check-In procedure"
            }
        }
    } catch (Exception e) {
        log.error "There was an error running the Check-In Procedure, ${e}"
        sendNotification "Failed to run the the Check-In procedure"
    }
}

def checkinPrepProcedure(forceEventOverride = false) {
    try {
        if(debugMode) log.debug "Executing the checkinPrepProcedure"
        def iCalData = getCalendarData(calendarUrl)
        def iCalDict = iCalToMapListAirBnB(iCalData)
        def checkinTodayData = checkinToday(iCalDict, forceEventOverride)
        if(checkinTodayData) {
            log.info "Running the AirBNB Check-In Prep Procedure"
            if(debugMode) log.debug "The data for today's Check-In is ${checkinTodayData}"
            location.setMode(checkinPrepMode)
            if(programLocksAtCheckinPrep) {
                doorLocks.each { lock ->
                    def nextAvailableCodePosition = findNextAvailableCodePosition(lock)
                    if(debugMode) log.debug "The next available code position returned is ${findNextAvailableCodePosition}"
                    if(nextAvailableCodePosition) {
                        if(debugMode) log.debug "Programming the door lock code now"
                        programDoorLockCode(lock, nextAvailableCodePosition.toInteger(), checkinTodayData.toString())
                        def checkIfCodeExists = findExistingAirbnbCodePosition(lock)
                        if(checkIfCodeExists) {
                            return
                        } else {
                            retries = retries + 1
                        }
                    }
                }
            }
            log.info "Successfully ran the Check-In Prep procedure and changed the mode to ${checkinPrepMode}"
            if(!notificationOnErrorsOnly) {
                sendNotification "Successfully ran the Check-In Prep procedure"
            }
        }
    } catch (Exception e) {
        log.error "There was an error running the Check-In Prep Procedure, ${e}"
        sendNotification "Failed to run the the Check-In Prep procedure"
    }
}

def checkoutProcedure(forceEventOverride = false) {
    try {
        if(debugMode) log.debug "Executing the checkoutProcedure"
        def iCalData = getCalendarData(calendarUrl)
        def iCalDict = iCalToMapListAirBnB(iCalData)
        def checkoutTodayData = checkoutToday(iCalDict, forceEventOverride)
        if(checkoutTodayData) {
            log.info "Running the AirBNB Check-Out Procedure"
            if(debugMode) log.debug "The data for today's Check-Out is ${checkoutTodayData}"
            location.setMode(checkoutMode)
            doorLocks.each { lock ->
                def checkIfCodeExists = findExistingAirbnbCodePosition(lock)
                if(checkIfCodeExists) {
                    if(debugMode) log.debug "Found the existing AirBNB code at position ${checkIfCodeExists}"
                    deleteDoorLockCode(lock, checkIfCodeExists)
                }
            }
            log.info "Successfully ran the Check-Out procedure and changed the mode to ${checkoutMode}"
            if(!notificationOnErrorsOnly) {
                sendNotification "Successfully ran the Check-Out procedure"
            }
        }
    } catch (Exception e) {
        log.error "There was an error running the Check-Out Procedure, ${e}"
        sendNotification "Failed to run the the Check-Out procedure"
    }
}

def getCalendarData(calendarUrl) {
    if(debugMode) log.debug "Getting URL ${calendarUrl}"
    try {
        httpGet(calendarUrl) { 
            resp -> if (resp.data) {
                resp.data = (resp.data instanceof String ? resp.data : (resp.data instanceof ByteArrayInputStream) ? new String(resp.data.buf) : "") // Convert the ByteArrayInputStream to a String
                def iCalData = resp.data?.trim()
                if(debugMode) log.debug "The raw iCal data is ${iCalData}"
                return iCalData
            } 
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error "Request failed for path: ${calendarUrl}.  ${e.response?.data}"
        return
    }
}

def testCalendarUrl(calendarUrl) {
    log.info "Testing the iCalendar data"
    try {
        def iCalData = getCalendarData(calendarUrl)
        def iCalDict = iCalToMapListAirBnB(iCalData)
        def iCalDataStr = iCalDict.join(", ")
        if(debugMode) log.debug "The iCal Parsed data is ${iCalDataStr}"
        log.info "Successfully verified ${calendarUrl}"
        state.testCalendarUrlState = true
    } catch (Exception e) {
        log.error "The iCalendar data could not be parsed. ${e}"
        state.testCalendarUrlState = false
        return
    }
}

void appButtonHandler(btn) {
    if(btn == "testCalendarUrl") testCalendarUrl(calendarUrl)
    if(btn == "disableButton") {
        disableAutomation()
        state.enabled = false
    }
    if(btn == "enableButton") {
        enableAutomation()
        state.enabled = true
    }
    if(btn == "sendTestNotification") {
        sendNotification("This is a test notification")
    }
    if(btn == "testCheckinPrepProcedure") {
        checkinPrepProcedure(forceEventOverride)
    }
    if(btn == "testCheckinProcedure") {
        checkinProcedure(forceEventOverride)
    }
    if(btn == "testCheckoutProcedure") {
        checkoutProcedure(forceEventOverride)
    }
    if(btn == "testDoorLockProgramming") {
        testDoorLockProgramming()
    }
}

def iCalToMapListAirBnB(str) {
    try {
        def events = []
        def eventMatches = (str =~ /BEGIN:VEVENT[\s\S]*?END:VEVENT/).collect()

        if(debugMode) log.debug "The eventMatches are ${eventMatches}"

        eventMatches.each { match ->
            def event = [:]

            if(debugMode) log.debug "The next line is ${match}"
            def eventText = match

            event.summary = extractProperty(eventText, "SUMMARY")
            //event.startDate = parseICalDate(extractProperty(eventText, "DTSTART;VALUE=DATE"))
            //event.endDate = parseICalDate(extractProperty(eventText, "DTEND;VALUE=DATE"))
            event.startDate = extractProperty(eventText, "DTSTART;VALUE=DATE")
            event.endDate = extractProperty(eventText, "DTEND;VALUE=DATE")
            event.phone = extractProperty(eventText, "Last 4 Digits.")

            events << event
        }

        return events
    } catch (Exception e) {
        log.error "Error parsing .ics file: ${e.message}"
        return null
    }
}

// Function to extract property values from iCalendar text
def extractProperty(eventText, propertyName) {
    def pattern = Pattern.compile(".*?${propertyName}:(.*?)\\r?\\n")
    def matcher = pattern.matcher(eventText)
    if (matcher.find()) {
        if(debugMode) log.debug "There was a match for ${matcher.group(1)}"
        return matcher.group(1)
    } else {
        if(debugMode) log.debug "There was no matched pattern for ${propertyName} in the string ${eventText}"
        return null
    }
}

// Function to parse iCalendar date string into a Date object
def parseICalDate(dateStr) {
    def dateFormat = new SimpleDateFormat("yyyyMMdd")
    return dateFormat.parse(dateStr)
}

void sendNotification(msg) {
    try {
        notificationDevices.each { device ->
            if(debugMode) log.debug "Sending a push notification to ${device} with message \"${msg}\""
            device.deviceNotification("AirBNB: ${msg}")
        }
    } catch (Exception e) {
        log.error "Unable to run the Sent Test Notification because there's no devices selected"
    }
}

def checkinToday(iCalData, forceEventOverride) {

    def eventIsToday = null
    def eventIsReserved = null
    def eventPhone = false

    for(event in iCalData) {
        eventIsToday = false
        eventIsReserved = false
        def todaysDate = new Date().format( 'yyyyMMdd' )
        if(debugMode) log.debug "Today's date is ${todaysDate}"
        if(debugMode) log.debug "The iCalData being analyzed for checkinToday is ${event}"
        for(item in event) {
            def itemStr = item.toString().split("=")
            if(itemStr[0]=="startDate") {
                if(debugMode) log.debug "The start Date is ${itemStr[1]}"
                if(todaysData == itemStr[1]) {
                    if(debugMode) log.debug "Found an event that starts today"
                    eventIsToday = true
                }
                if(forceEventOverride) {
                    if(debugMode) log.debug "Force Event Override: Found an event that starts today"
                    eventIsToday = true
                }
            }
            if(itemStr[0]=="summary") {
                if(debugMode) log.debug "The event summary is ${itemStr[1]}"
                if(itemStr[1]=="Reserved") {
                    if(debugMode) log.debug "Found an event is a summary of Reserved"
                    eventIsReserved = true
                }
            }
            if(itemStr[0]=="phone") {
                if(debugMode) log.debug "The phone is ${itemStr[1]}"
                eventPhone = itemStr[1].toInteger()
            }
        }
        if(forceEventOverride) log.debug "The status of eventIsToday is ${eventIsToday} and eventIsReserved is ${eventIsReserved}"
        if(eventIsToday && eventIsReserved && eventPhone) {
            if(debugMode) log.debug "Found a suitable event. Breaking the loop"
            break
        }
    }
    if(eventIsToday && eventIsReserved && eventPhone) {
        if(debugMode) log.debug "The checkinToday function ran and found a reservation"
        return eventPhone
    }
    if(debugMode) log.debug "The checkinToday function ran and didn't find any reservation"
    return false
}

def checkoutToday(iCalData, forceEventOverride) {

    def eventIsToday = null
    def eventIsReserved = null

    for(event in iCalData) {
        eventIsToday = false
        eventIsReserved = false
        def todaysDate = new Date().format( 'yyyyMMdd' )
        if(debugMode) log.debug "Today's date is ${todaysDate}"
        if(debugMode) log.debug "The iCalData being analyzed for checkoutToday is ${event}"
        for(item in event) {
            def itemStr = item.toString().split("=")
            if(itemStr[0]=="endDate") {
                if(debugMode) log.debug "The end Date is ${itemStr[1]}"
                if(todaysData == itemStr[1]) {
                    if(debugMode) log.debug "Found an event that ends today"
                    eventIsToday = true
                }
                if(forceEventOverride) {
                    if(debugMode) log.debug "Force Event Override: Found an event that ends today"
                    eventIsToday = true
                }
            }
            if(itemStr[0]=="summary") {
                if(debugMode) log.debug "The event summary is ${itemStr[1]}"
                if(itemStr[1]=="Reserved") {
                    if(debugMode) log.debug "Found an event is a summary of Reserved"
                    eventIsReserved = true
                }
            }
        }
        if(forceEventOverride) log.debug "The status of eventIsToday is ${eventIsToday} and eventIsReserved is ${eventIsReserved}"
        if(eventIsToday && eventIsReserved) {
            if(debugMode) log.debug "Found a suitable event. Breaking the loop"
            break
        }
    }
    if(eventIsToday && eventIsReserved) {
        if(debugMode) log.debug "The checkinToday function ran and found a reservation"
        return true
    }
    if(debugMode) log.debug "The checkinToday function ran and didn't find any reservation"
    return false
}

def findExistingAirbnbCodePosition(lock) {
    int airbnbCodePosition = 0
    Boolean codeAvailable = false
    if(debugMode) log.debug "Getting the door lock codes"
    def codes = lock.currentState("lockCodes").value
    if(debugMode) log.debug "The door lock codes from ${lock} are ${codes}"
    def jsonSlurper = new JsonSlurper()
    def codeJson = jsonSlurper.parseText(codes)
    for(codePosition in codeJson) {
        if(debugMode) log.debug "The code position is ${codePosition.value} in ${codePosition.key}"
        for(codeData in codePosition.value) {
            if(codeData.key == "name") {
                if(debugMode) log.debug "The code name is ${codeData.value}"
                if(codeData.value == "AirBNB") {
                    if(debugMode) log.debug "KEY FOUND at position ${codePosition.key}"
                    airbnbCodePosition = codePosition.key.toInteger()
                    codeAvailable = true
                    break
                }
            }
        }
        if(codeAvailable) break
    }
    if(codeAvailable) {
        if(debugMode) log.debug "The availaable code position being returned is ${airbnbCodePosition}"
        return airbnbCodePosition
    }
    return null
}

def findNextAvailableCodePosition(lock) {
    int availableCodePosition = 1
    Boolean codeAvailable = false
    def codes = lock.currentState("lockCodes").value
    if(debugMode) log.debug "The lockCodes are ${codes}"
    int maxCodes = lock.currentState("maxCodes").value.toInteger()
    if(debugMode) log.debug "The maximum number of codes allowed ${maxCodes}"
    def jsonSlurper = new JsonSlurper()
    def codeJson = jsonSlurper.parseText(codes)
    def codeJsonCount = codeJson.size()
    if(debugMode) log.debug "The size of the codeJson is ${codeJsonCount}"
    while(availableCodePosition<=codeJsonCount) {
        if(maxCodes == availableCodePosition) {
            if(debugMode) log.debug "AVAILABLE CODE NOT FOUND: Returning with null"
            return null
        }
        availableCodePosition++
    }
    if(debugMode) log.debug "AVAILABLE CODE FOUND: Returning with ${availableCodePosition}"
    return availableCodePosition
}

def programDoorLockCode(lock, position, code) {
    if(debugMode) log.debug "The lock is ${lock}"
    if(debugMode) log.debug "The position is ${position}"
    if(debugMode) log.debug "The code is ${code}"
    try {
        lock.setCode(codeposition = position, pincode = code, name = "AirBNB")
    } catch(Exception e) {
        log.error "There was an error programming the door lock code, ${e}"
        return false
    }
    return true
}

def deleteDoorLockCode(lock, position) {
    if(debugMode) log.debug "The lock is ${lock}"
    if(debugMode) log.debug "The position is ${position}"
    try {
        lock.deleteCode(position.toInteger())
    } catch(Exception e) {
        log.error "There was an error programming the door lock code, ${e}"
        return false
    }
    return true
}
