var me = this;
var ME = $('#'+me.UUID)[0];

me.data = {};

if (ME.DATA.peer && ME.DATA.peer != 'local') {
  me.peer = ME.DATA.peer;
  me.prefix = '../peer/remote/'+me.peer+'/';
}
else me.prefix = '../';

me.ready = function(){
  var el = $('<div class="raspi_meminfo iconmode"/>');
  updateDial(el);
};

function updateDial(el){
  json(me.prefix+'raspberry/memory', null, function(result){
    var total = result.data.MemTotal;
    var free = result.data.MemAvailable;
    var p = 100-(100*free/total);
    
    var unit = "kb";
    var base = 1;
    if (total > base * 1024) { base *= 1024; unit = "mb"; }
    if (total > base * 1024) { base *= 1024; unit = "gb"; }
    if (total > base * 1024) { base *= 1024; unit = "tb"; }
    
    if (!$(ME).find(el[0])[0]){
      $(ME).find('.dialgoeshere').html(el);
      var title = ME.DATA.title ? ME.DATA.title : "Memory Used";
      var d = {
        value: p.toFixed(1),
        units: "% of "+(total/base).toFixed(1)+unit,
        title: title,
        range: [0,100]
      };
      installControl(el[0], "app", "dial", function(api){
        setTimeout(function(){
          updateDial(el);
        }, 3000);
      }, d);
    }
    else {
      var api = el[0].api;
      api.update(p.toFixed(1));
      setTimeout(function(){
        updateDial(el);
      }, 3000);
    }  
  });
}
