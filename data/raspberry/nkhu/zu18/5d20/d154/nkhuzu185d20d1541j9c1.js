var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
  $(ME).find('.allpeersbutton').click(function(){
    $(ME).find('.updateresult').text("");
    $(ME).find('.updateerror').text("");
    var list = $(ME).find('.whichpeer').find('select').find('option');
    var i = list.length;
    while (i-->1){
      var name = list[i].innerHTML;
      var peer = list[i].value;
      upgradePeer(peer, name);
    }
  });
  $(ME).find('.all3button').click(function(){
    $(ME).find('.updateresult').text("sending update command...");
    $(ME).find('.updateerror').text("");
    var peer = $(ME).find('.whichpeer').find('select').val();
    if (peer == 'local') peer = null;
    send_update(function(result){
      if (result.status != 'ok') {
        $(ME).find('.updateerror').text(result.msg);
        $(ME).find('.updateresult').text("");
      }
      else{
        $(ME).find('.updateresult').text(result.data.out);
        $(ME).find('.updateerror').text(result.data.err);
        var list = result.data.err.split("\n");
        var i = list.length;
        while (i-->0) if (list[i].startsWith("E:")) return;
        $(ME).find('.updateresult').append('<br><br><i>listing updates...</i>');
        send_list_available(function(result){
          if (result.status != 'ok') {
            $(ME).find('.updateerror').text(result.msg);
            $(ME).find('.updateresult').text("");
          }
          else{
            $(ME).find('.updateresult').text(result.data.out);
            $(ME).find('.updateerror').text(result.data.err);
            var list = result.data.out.split("\n");
            var i = list.length;
            var n = 0;
            while (i-->0) if (list[i].startsWith("Inst ")) n++;
            if (n>0){
              $(ME).find('.updateresult').html('<i>fetching '+n+' updates...</i>');
              send_upgrade(function(result){
                if (result.status != 'ok') {
                  $(ME).find('.updateerror').text(result.msg);
                  $(ME).find('.updateresult').text("");
                }
                else{
                  $(ME).find('.updateresult').text(result.data.out);
                  $(ME).find('.updateerror').text(result.data.err);
                }
              }, peer);
            }
          }
        }, peer);
      }
    }, peer);
  });
  $(ME).find('.updatebutton').click(function(){
    $(ME).find('.updateresult').text("sending update command...");
    $(ME).find('.updateerror').text("");
    var peer = $(ME).find('.whichpeer').find('select').val();
    if (peer == 'local') peer = null;
    send_update(function(result){
      if (result.status != 'ok') {
        $(ME).find('.updateerror').text(result.msg);
        $(ME).find('.updateresult').text("");
      }
      else{
        $(ME).find('.updateresult').text(result.data.out);
        $(ME).find('.updateerror').text(result.data.err);
      }
    }, peer);
  });
  $(ME).find('.upgradebutton').click(function(){
    $(ME).find('.updateresult').text("sending upgrade command...");
    $(ME).find('.updateerror').text("");
    var peer = $(ME).find('.whichpeer').find('select').val();
    if (peer == 'local') peer = null;
    send_upgrade(function(result){
      if (result.status != 'ok') {
        $(ME).find('.updateerror').text(result.msg);
        $(ME).find('.updateresult').text("");
      }
      else{
        $(ME).find('.updateresult').text(result.data.out);
        $(ME).find('.updateerror').text(result.data.err);
      }
    }, peer);
  });
  $(ME).find('.listbutton').click(function(){
    $(ME).find('.updateresult').text("sending list_available command...");
    $(ME).find('.updateerror').text("");
    var peer = $(ME).find('.whichpeer').find('select').val();
    if (peer == 'local') peer = null;
    send_list_available(function(result){
      if (result.status != 'ok') {
        $(ME).find('.updateerror').text(result.msg);
        $(ME).find('.updateresult').text("");
      }
      else{
        $(ME).find('.updateresult').text(result.data.out);
        $(ME).find('.updateerror').text(result.data.err);
      }
    }, peer);
  });
  $(ME).find('.osinfobutton').click(function(){
    $(ME).find('.updateresult').text("sending os_info command...");
    $(ME).find('.updateerror').text("");
    var peer = $(ME).find('.whichpeer').find('select').val();
    if (peer == 'local') peer = null;
    send_os_info(function(result){
      if (result.status != 'ok') {
        $(ME).find('.updateerror').text(result.msg);
        $(ME).find('.updateresult').text("");
      }
      else{
        var s = '';
        for (var key in result.data)
          s += key + ': ' + result.data[key] + '<br>';
        $(ME).find('.updateresult').html(s);
        $(ME).find('.updateerror').text("");
      }
    }, peer);
  });
  $(ME).find('.listallbutton').click(function(){
    $(ME).find('.updateresult').text("");
    $(ME).find('.updateerror').text("");
    var list = $(ME).find('.whichpeer').find('select').find('option');
    var i = list.length;
    while (i-->1){
      var name = list[i].innerHTML;
      var peer = list[i].value;
      check_os(peer, name);
    }
  });
  $(ME).find('.addkeybutton').click(function(){
    var key = $(ME).find('.keyid').val();
    var peer = $(ME).find('.whichpeer').find('select').val();
    if (peer == 'local') peer = null;
    send_add_key(key, function(result){
      if (result.status != 'ok') {
        $(ME).find('.updateerror').text(result.msg);
        $(ME).find('.updateresult').text("");
      }
      else{
        $(ME).find('.updateresult').text(result.data.out);
        $(ME).find('.updateerror').text(result.data.err);
      }
    }, peer);
  });
};

function extractErrors(msg){
  var list = msg.split("\n");
  var i = list.length;
  var list2 = [];
  while (i-->0) if (list[i].startsWith("E:")) list2.push(list[i]);
  return list2;
}

function extractUpdates(msg){
  var n = 0;
  var list = msg.split("\n");
  var i = list.length;
  while (i-->0) if (list[i].startsWith("Inst ")) n++;
  return n;
}

function displayErrors(el, list){
  el.html('');
  i = list.length;
  while (i-->0){
    el.append('<br>'+red(list[i]));
  }
}

function upgradePeer(peer, name){
  $(ME).find('.updateresult').append(name+": <span id='up_"+peer+"'><i>Updating package lists...</i></span><br>");
  send_update(function(result){
    var el = $(ME).find('#up_'+peer);
    if (result.status != 'ok') el.html('<br>'+red(result.msg));
    else{
      var list = extractErrors(result.data.err);
      if (list.length>0) displayErrors(el, list);
      else{
        el.html('<i>Calculating available updates...</i>');
        send_list_available(function(result){
          if (result.status != 'ok') el.html('<br>'+red(result.msg));
          else{
            var list = extractErrors(result.data.err);
            if (list.length>0) displayErrors(el, list);
            else{
              var n = extractUpdates(result.data.out);
              if (n == 0) el.html('OK');
              else {
                el.html('<i>fetching '+n+' updates...</i>');
                send_upgrade(function(result){
                  if (result.status != 'ok') el.html('<br>'+red(result.msg));
                  else{
                    var list = extractErrors(result.data.err);
                    if (list.length>0) displayErrors(el, list);
                    else el.html('OK ('+n+' updated)');
                  }
                }, peer);
              }
            }
          }
        }, peer);
      }
    }
  }, peer);
}

function check_os(peer, name){
  $(ME).find('.updateresult').append("<div id='os_"+peer+"'><i>Checking OS on "+name+"...</i></div>");
  send_os_info(function(result){
    console.log(result);
    var el = $(ME).find('#os_'+peer);
    if (result.status != 'ok')
      el.html(red(name+' = '+result.msg));
    else{
      el.text(name+' = '+result.data.PRETTY_NAME);
    }
  }, peer);
}

function red(msg){
  return "<font color='red'>"+msg+"</font>";
}
