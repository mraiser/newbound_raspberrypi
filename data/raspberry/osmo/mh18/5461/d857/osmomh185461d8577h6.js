var me = this; 
var ME = $('#'+me.UUID)[0];

me.data = {};

if (ME.DATA.peer && ME.DATA.peer != 'local') {
  me.peer = ME.DATA.peer;
  me.prefix = '../peer/remote/'+me.peer+'/';
}
else me.prefix = '../';

me.ready = function(){
  json(me.prefix+'raspberry/temp', null, function(result){
    var tempc = result.data.temp_c;
    var tempf = result.data.temp_f;
    var v = tempf.toFixed ? tempf.toFixed(1) : "--";
    
    var title = ME.DATA.title ? ME.DATA.title : "CPU Temp";
    ME.DATA.title = title;
    var d = {
      value: v,
      units: "f",
      title: title,
      range: [-30,130]
    };
    
    var el = $('<div class="raspitemp iconmode"/>');
    $(ME).find('.dialgoeshere').html(el);
    el.data("data", d);
    installControl(el[0], "app", "dial", function(api){
      updateDial(el);
    }, d);
    
  });
};
  
function updateDial(el){
  if ($('#'+me.UUID)[0] == ME){
    var d = el.data("data");
    var api = el[0].api;
    json(me.prefix+'raspberry/temp', null, function(result){
      var v = "--";
      if (result.msg) {
        var i = result.msg.indexOf('=');
        var x = result.msg.indexOf("'");
        var tempc = Number(result.msg.substring(i+1,x));
        var tempf = (tempc * 9/5) + 32;
        v = tempf.toFixed(1);
      }
      api.update(v);
      setTimeout(function(){
        updateDial(el);
      }, 3000);
    });
  }
}

$(document).click(function(event) {
   window.lastElementClicked = event.target;
});
