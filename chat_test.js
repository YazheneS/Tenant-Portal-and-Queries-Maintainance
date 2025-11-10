const { Builder, By } = require('selenium-webdriver');
const assert = require('assert');

(async function testChatValidation() {
  let driver = await new Builder().forBrowser('chrome').build();

  try {
    //await driver.get('http://127.0.0.1:3000/f51fce2d-3e38-43d5-9f73-8b0c10f5bfc8.html');
    await driver.get('http://127.0.0.1:3000/chat.html');

    const nameInput = await driver.findElement(By.id('sender'));
    const msgInput = await driver.findElement(By.id('content'));
    const sendButton = await driver.findElement(By.id('sendButton'));
    const deleteButton = await driver.findElement(By.id('deleteLatest'));

    async function clearAndType(el, text) {
      await el.clear();
      await el.sendKeys(text);
    }

    async function logInputValues(nameVal, msgVal) {
      console.log('\nTesting with inputs:');
      console.log('   Name   →', `"${nameVal}"`);
      console.log('   Message→', `"${msgVal}"`);
    }

    // 1️ Invalid name (special characters)
    let nameVal = 'Jo@hn';
    let msgVal = 'This is a valid message length';
    await logInputValues(nameVal, msgVal);
    await clearAndType(nameInput, nameVal);
    await clearAndType(msgInput, msgVal);
    await driver.sleep(1000);
    let isSendEnabled = await sendButton.isEnabled();
    console.log('Send enabled (invalid name):', isSendEnabled);
    assert.strictEqual(isSendEnabled, false);

    // 2️ Short name (<3 chars)
    nameVal = 'Al';
    msgVal = 'This is a valid message length';
    await logInputValues(nameVal, msgVal);
    await clearAndType(nameInput, nameVal);
    await clearAndType(msgInput, msgVal);
    await driver.sleep(1000);
    isSendEnabled = await sendButton.isEnabled();
    console.log('Send enabled (too short name):', isSendEnabled);
    assert.strictEqual(isSendEnabled, false);

    // 3️ Valid name but short message (<10 chars)
    nameVal = 'Alice';
    msgVal = 'Hi';
    await logInputValues(nameVal, msgVal);
    await clearAndType(nameInput, nameVal);
    await clearAndType(msgInput, msgVal);
    await driver.sleep(1000);
    isSendEnabled = await sendButton.isEnabled();
    console.log('Send enabled (too short message):', isSendEnabled);
    assert.strictEqual(isSendEnabled, false);

    // 4️ Valid name & valid message
    nameVal = 'Alice';
    msgVal = 'Hello everyone, this is a valid message!';
    await logInputValues(nameVal, msgVal);
    await clearAndType(nameInput, nameVal);
    await clearAndType(msgInput, msgVal);
    await driver.sleep(1000);
    isSendEnabled = await sendButton.isEnabled();
    console.log('Send enabled (valid fields):', isSendEnabled);
    assert.strictEqual(isSendEnabled, true);

    // 5️ Click send and confirm message appears
    await sendButton.click();
    console.log('Message sent successfully.');
    const chatMessages = await driver.findElement(By.id('chatMessages'));
    const text = await chatMessages.getText();
    assert(text.includes('Alice'));

    // 6️ Delete last message (should work because it’s <60 min old)
    await deleteButton.click();
    console.log('Delete button clicked.');
    await driver.sleep(1000);
    const chatTextAfterDelete = await chatMessages.getText();
    console.log('Chat after delete:', chatTextAfterDelete);

    console.log('All test cases passed successfully!');
  } catch (err) {
    console.error('Test failed:', err);
  } finally {
    await driver.quit();
  }
})();

//cd C:\Users\YAZHENE\Desktop\3rd semester\Mini project SWE
//npx http-server . -p 3000

//http://127.0.0.1:3000/chat.html

//cd C:\Users\YAZHENE\Desktop\3rd semester\Mini project SWE
//node chat_test.js
