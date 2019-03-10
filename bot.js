const Discord = require('discord.js');
const client = new Discord.Client();

client.on('ready', () => {
    console.log('I am ready!');
});

client.on('message', message => {
    if (message.content === 'Holo sight on ak') {
    	message.reply('EWW');
        
        client.on('message', message => {
    if (message.content === 'ur mom gay') {
    	message.reply('no u');
  	}
});

// THIS  MUST  BE  THIS  WAY
client.login(process.env.BOT_TOKEN);
