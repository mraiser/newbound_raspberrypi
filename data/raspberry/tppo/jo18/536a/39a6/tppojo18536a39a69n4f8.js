var me = this;
var ME = $('#'+me.UUID)[0];

me.data = {};

if (ME.DATA.peer && ME.DATA.peer != 'local') {
  me.peer = ME.DATA.peer;
  me.prefix = '../peer/remote/'+me.peer+'/';
}
else me.prefix = '../';

me.ready = function(){
  var el = $('<div class="raspi_diskinfo iconmode"/>');
  updateDial(el);
};

function updateDial(el){
  if ($(document).find(ME)[0]) json(me.prefix+'raspberry/disks', null, function(result){
    var which = ME.DATA.disk ? ME.DATA.disk : "/";
    var disk = getByProperty(result.data, "Mounted", which);
    //var p = disk["Use%"];
    var el = $('<div class="raspi_diskinfo iconmode"/>');
    var total = disk['1K-blocks'] * 1000;
    var unused = disk.Available * 1000;
    var title = ME.DATA.title ? ME.DATA.title : "Disk Used";
    var unit = "b";
    var base = 1;
    if (total > base * 1024) { base *= 1024; unit = "kb"; }
    if (total > base * 1024) { base *= 1024; unit = "mb"; }
    if (total > base * 1024) { base *= 1024; unit = "gb"; }
    if (total > base * 1024) { base *= 1024; unit = "tb"; }
console.log(disk);
    var p = 100 * (total - unused) / total;

    if (!$(ME).find(el[0])[0]){
      $(ME).find('.dialgoeshere').html(el);
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
  }, me.peer);
}