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
    if (message.content === '@[TPD] Clan Bot
#5633') {
    	message.reply('Hello! ;-) ');
  	}
});

});

client.on('message', message => {
    if (message.content === '@sporky#3385') {
    	message.reply('Dont @ him >;(');
  	}
});


});

client.on('message', message => {
    if (message.content === '@webcrawler#5282') {
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
