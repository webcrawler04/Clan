const Discord = require('discord.js');
const client = new Discord.Client();

client.on('ready', () => {
    console.log('I am ready!');
});

client.on('message', message => {
    if (message.content === 'ping') {
    	message.reply('pong');
        
        client.on('message', message => {
    if (message.content === 'Hello') {
    	message.reply('Hello, nice to meet you!');
        
        client.on('message', message => {
    if (message.content === 'ur mom gey') {
    	message.reply('no u');
        
        client.on('message', message => {
    if (message.content === 'Holo sight on ak.') {
    	message.reply('EWW');
  	}
});

// THIS  MUST  BE  THIS  WAY
client.login(process.env.BOT_TOKEN);
