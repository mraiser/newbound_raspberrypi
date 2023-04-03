var me = this; 
var ME = $('#'+me.UUID)[0];

me.uiReady = function(ui){
  me.ui = ui;
  $(ME).find('.wrap').css('display', 'block');
};

me.ready = function(ui){
  send_info(function(result){
    var sys = result.data.system;
    if (sys == "UNKNOWN DEVICE") sys = "Not a Raspberry Pi";
    $(ME).find('.sysname').text(sys);
    var os = result.data.os;
    var name = os.NAME + " ("+os.ID_LIKE+"/"+os.ID+") "+os.VERSION_ID+" "+os.VERSION_CODENAME;
    $(ME).find('.osname').text(name);
    var cpu = result.data.arch+ " " +result.data.cpu.count+" cores";
    $(ME).find('.cpu').text(cpu);
    var gb = (Number(result.data.memory.MemTotal)/1024000).toFixed(1);
    $(ME).find('.memory').text(gb+"gb RAM");
    var disks = "";
    for (var i in result.data.disks) {
      var d = result.data.disks[i];
      if (d.Filesystem && d.Filesystem.indexOf('tmpfs') == -1) {
        var gb = Number(d['1K-blocks'])/1024000;
        if (gb > 1) {
          disks += gb.toFixed(1) + "gb " +d.Mounted + '<br>';
        }
      }
    }
    $(ME).find('.disks').html(disks);
  });
};