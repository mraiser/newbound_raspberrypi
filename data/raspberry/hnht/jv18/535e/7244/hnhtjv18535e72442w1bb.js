var me = this;
var ME = $('#'+me.UUID)[0];

me.data = {};

if (ME.DATA.peer && ME.DATA.peer != 'local') {
  me.peer = ME.DATA.peer;
  me.prefix = '../peer/remote/'+me.peer+'/';
}
else me.prefix = '../';

me.ready = function(){
  var el = $('<div class="raspi_cpuinfo iconmode"/>');
  updateDial(el);
};

function updateDial(el){
  if ($(document).find(ME)[0]) json(me.prefix+'raspberry/cpu', null, function(result){
    var p = result.data.usage*100;
    if (!$(ME).find(el[0])[0]){
      $(ME).find('.dialgoeshere').html(el);
      var title = ME.DATA.title ? ME.DATA.title : "CPU Used";
      var d = {
        value: p.toFixed(1),
        units: '% of '+result.data.count+' cores',
        title: title,
        range: [0,100]
      };
      installControl(el[0], "app", "dial", function(api){
        setTimeout(function(){
          updateDial(el, false);
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
