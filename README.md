# Hubitat AirBNB Automator by [Dan Healy](https://thedanhealy.com)

This Hubitat app provides a direct integration with AirBNB and control over changing modes and programming door locks **without the use of any other services, such as the Maker API and/or other applications**. 

I wrote this app for a non-profit organization which I serve on the Board of Directors as the Vice President. We operate a 49 acre property in Ohio, called the Minton Lodge, that we offer to families who have gone through cancer or military journeys for completely free as a therapeutic and relaxation retreat. I originally outfitted the Minton Lodge with SmartThings because the mobile app interface was easier for my fellow Board members and volunteers to learn, and we used the famous Rental Lock Automator from RBoy. But, since Jan 2022 when SmartThings stopped supporting the Groovy-based apps platform and RBoy didn't provide any path forward, we started inputting door lock codes manually and carefully arming/disarming the lodge.

If you enjoy using this app, please consider donating to the **[Josh Minton Foundation](https://brotherson3.org)**. I created this app to help our foundation better manage the AirBNB guests, which provides us with extra funding inbetween our no-cost therapeutic stays. I am now offering this app to the community for free, but in hopes that you'll also donate back in appreciation for the free usage.

**This app only works with the AirBNB calendar at the present time. I am committed to continuing to update this app with new functionality and review all pull requests submitted.**

# Prerequisites

The following items are required for this app to operate with your Hubitat:

- Programmable Door Lock
- AirBNB Calendar URL (Learn more [here](https://www.airbnb.com/help/article/99#section-heading-9-0))
- "Mode" in Hubitat that you want to use for Check-In, Check-In Prep (that's a certain amount of time before check-in when you want to start doing something, like cooling or heating), and Check-Out. You can learn about how to do this [here](https://docs2.hubitat.com/how-to/add-or-change-a-mode#:~:text=In%20order%20to%20start%20using,in%20the%20list%20of%20settings).

# Setup Instructions

1. Log into your Hubitat, go to "Apps Code" and click the button for "+ New App"
1. Click the "Import" button, paste the URL https://raw.githubusercontent.com/TheDanHealy/hubitat-airbnb-automator/main/hubitat-airbnb-automator.groovy, click the button "Import", click "OK to confirm you want to overwrite, and finally click the "Save" button.
1. In the Hubitat menu, go to "Apps", click the button "+ Add User App", and finally click on "AirBNB Automator"
1. Supply all the required information in the settings page and click the button "Save"
1. Click the button "Test" to test and verify there's no issues with your AirBNB Calendar URL
1. If everything looks OK, finally, click the button "Enable AirBNB Automation"

That's it :)

# Having any issue?

If you're having any issues with the app, please first enable Debug mode, view the [logs](https://docs2.hubitat.com/how-to/collect-information-for-support#:~:text=Open%20your%20logs%20by%20selecting,depending%20on%20your%20log%20settings), and repeat the issue. Review the logs

If you want / need to submit any issue, please click on the tab above "Issues" and create a new GitHub Issue.

# Message for developers

I encourage you to plese help build and improve this app. Fork this repo, add enhancements, fixes, and improvements, then submit a PR back to here.

