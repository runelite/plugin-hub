# Boost Notifier

**Boost Notifier** is a RuneLite plugin that notifies players when their skill boost for a selected skill (e.g., ) drops to specified levels. The plugin allows users to set multiple thresholds for skill boosts and receive both in-game and email notifications when these thresholds are met.

## Features

- **Multiple Skill Thresholds**: Set up to 5 different boost thresholds for a specific skill.
- **Skill Monitoring**: Select which skill to monitor for boost notifications.
- **In-game Notifications**: Get a RuneLite notification when the boost reaches one of the set thresholds.
- **Email Notifications**: Optionally receive an email notification with customizable subject when a boost threshold is reached.
- **Customizable Settings**: Configure boost thresholds, email settings, and notification preferences directly from the RuneLite client.

## Installation

1. Download and install the RuneLite client from the [official website](https://runelite.net/).
2. Add the **Boost Notifier** plugin via the Plugin Hub in RuneLite:
   - Navigate to `Configuration` > `Plugin Hub`.
   - Search for "Boost Notifier" and click `Install`.

## How to Use

1. **Open the Plugin Settings**:
   - Open RuneLite.
   - Go to `Configuration` and find `Boost Notifier`.

2. **Configure Skill Monitoring**:
   - **Skill to Monitor**: Select the skill you want to receive boost notifications for (e.g., , Attack).
   
3. **Set Boost Thresholds**:
   - Set up to 5 different boost thresholds using the `Boost Threshold` inputs.
   - Check the `Enable` box for each threshold you wish to activate.

4. **Enable Email Notifications (Optional)**:
   - **Enable Email Notifications**: Check this box to enable email alerts.
   - **Recipient Email Address**: The email address where notifications will be sent.
   - **Sender Email Address**: The email address used to send the notifications.
   - **Sender Email Password**: The password for the sender email (use an app password if using Gmail with 2FA).
   - **SMTP Server**: Set to `smtp.gmail.com` for Gmail or other SMTP server.
   - **SMTP Port**: Use port `465` for SSL/TLS (or another as per your email provider).

## Example Configuration

| Setting                  | Value                           |
|--------------------------|---------------------------------|
| Skill to Monitor         |                         |
| Boost Threshold 1        | 5                               |
| Enable Threshold 1       | ☑                               |
| Boost Threshold 2        | 3                               |
| Enable Threshold 2       | ☑                               |
| Recipient Email Address  | your-email@example.com          |
| Sender Email Address     | your-gmail@gmail.com            |
| Sender Email Password    | YourAppPassword                 |
| SMTP Server              | smtp.gmail.com                  |
| SMTP Port                | 465                             |

## Important Notes

- When using Gmail as the sender, ensure you have [enabled access](https://myaccount.google.com/security) for less secure apps or use an [app password](https://support.google.com/accounts/answer/185833?hl=en) if you have 2FA enabled.
- Make sure your thresholds and email settings are correctly configured to receive proper notifications.

## Feedback and Support

If you encounter any issues or have suggestions for improvements, please open an issue in the [GitHub repository](https://github.com/YOUR_USERNAME/YOUR_REPO).

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT).
