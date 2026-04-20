---
title: "Contact"
permalink: /contact/
layout: single
sidebar: false
---

Have a question about the framework, an idea for improvement, or a partnership proposal? I'd love to hear from you.

Feel free to reach out if you want to:
- Ask questions about **JsonApi4j** or get help with integration
- Discuss ideas, feature requests, or contributions
- Explore partnership or consulting opportunities
- Talk about API design, software architecture, or JSON:API in general

### Community Discussions

For technical questions and community conversations, visit [GitHub Discussions](https://github.com/MoonWorm/jsonapi4j/discussions).

### Send a Message

<form action="https://formsubmit.co/aliaksei.taliuk@gmail.com" method="POST">
  <!-- Honeypot spam protection -->
  <input type="text" name="_honey" style="display:none">
  <!-- Disable CAPTCHA (remove this line if you want CAPTCHA enabled) -->
  <input type="hidden" name="_captcha" value="false">
  <!-- Redirect back to the site after submission -->
  <input type="hidden" name="_next" value="https://api4.pro/contact-thank-you/">
  <!-- Subject line for emails -->
  <input type="hidden" name="_subject" value="JsonApi4j Contact Form">

  <div style="margin-bottom: 1em;">
    <label for="name" style="display: block; font-weight: 600; margin-bottom: 0.3em;">Name</label>
    <input type="text" id="name" name="name" required
           style="width: 100%; max-width: 400px; padding: 0.5em; border: 1px solid #ccc; border-radius: 4px;">
  </div>

  <div style="margin-bottom: 1em;">
    <label for="email" style="display: block; font-weight: 600; margin-bottom: 0.3em;">Email</label>
    <input type="email" id="email" name="email" required
           style="width: 100%; max-width: 400px; padding: 0.5em; border: 1px solid #ccc; border-radius: 4px;">
  </div>

  <div style="margin-bottom: 1em;">
    <label for="message" style="display: block; font-weight: 600; margin-bottom: 0.3em;">Message</label>
    <textarea id="message" name="message" rows="6" required
              style="width: 100%; max-width: 600px; padding: 0.5em; border: 1px solid #ccc; border-radius: 4px;"></textarea>
  </div>

  <button type="submit" class="btn btn--primary">Send Message</button>
</form>
