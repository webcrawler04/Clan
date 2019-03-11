const Discord = require('discord.js');
const client = new Discord.Client();

client.on('ready', () => {
    console.log('I am ready!');
});

client.on('message', message => {
    if (message.content === 'ping') {
    	message.reply('pong');
  	}
});

});

client.on('message', message => {
    if (message.content === '@soggie salad') {
    	message.reply('Hello! ;-) ');
  	}
});

});

client.on('message', message => {
    if (message.content === '@[TPD] sporky') {
    	message.reply('Dont @ him >;(');
  	}
});


});

client.on('message', message => {
    if (message.content === '@[TPD] web the bot.') {
    	message.reply('Dont @ him >;(');
  	}
});


});

client.on('message', message => {
    if (message.content === 'Holo sight on ak') {
    	message.reply('***This man is retarded...***');
  	}
});

// THIS  MUST  BE  THIS  WAY
client.login(process.env.BOT_TOKEN);
