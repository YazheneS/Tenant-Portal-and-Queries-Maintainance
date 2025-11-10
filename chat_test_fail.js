const { Builder, By } = require('selenium-webdriver');
const assert = require('assert');

(async function testChatValidationFailDemo() {
  let driver = await new Builder().forBrowser('chrome').build();

  try {
    //await driver.get('http://127.0.0.1:3000/f51fce2d-3e38-43d5-9f73-8b0c10f5bfc8.html');
    await driver.get('http://127.0.0.1:3000/chat.html');
    
    const nameInput = await driver.findElement(By.id('sender'));
    const msgInput = await driver.findElement(By.id('content'));
    const sendButton = await driver.findElement(By.id('sendButton'));

    // Helper functions
    async function clearAndType(el, text) {
      await el.clear();
      await el.sendKeys(text);
    }

    // 🧪 Intentionally wrong expectation:
    const invalidName = 'Jo@hn';
    const validMsg = 'This is a valid message length';

    console.log('\n🧪 Testing with invalid name but expecting button to be enabled (this will fail)...');
    await clearAndType(nameInput, invalidName);
    await clearAndType(msgInput, validMsg);
    await driver.sleep(1000);

    const isSendEnabled = await sendButton.isEnabled();
    console.log('Send button actual state:', isSendEnabled);

    // ❌ Force an incorrect expectation to trigger failure
    assert.strictEqual(isSendEnabled, true, 'Expected Send button to be enabled — but it is disabled!');

    console.log('\n✅ Test unexpectedly passed (this line should not appear).');

  } catch (err) {
    console.error('\n❌ Test failed as expected!');
    console.error('Error message:', err.message);

    // Optional: Save screenshot of failure
    try {
      const data = await driver.takeScreenshot();
      require('fs').writeFileSync('failure_demo.png', data, 'base64');
      console.log('🖼️ Screenshot saved: failure_demo.png');
    } catch (ssErr) {
      console.error('Could not save screenshot:', ssErr);
    }
  } finally {
    await driver.quit();
  }
})();
