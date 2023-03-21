var me = this; 
var ME = $('#'+me.UUID)[0];

if (ME.DATA.peer){
  if (ME.DATA.peer != 'local') me.peer = ME.DATA.peer;
}
else me.peer = typeof CURRENTDEVICEID == 'undefined' ? null : CURRENTDEVICEID;

//me.peer = "56ef9ae2-6fd9-40f7-8f46-0a3bcc711da5";

me.save = function(cb){
  var start = $(ME).find('.dvrs-start').prop('checked');
  var mot = $(ME).find('.dvrs-mot-start').prop('checked');
  var res = me.resselect ? me.resselect.value().split('x') : ["640","480"];
  var rot = me.rotselect ? Number(me.rotselect.value()): 0;
  var fps = me.fpsselect ? Number(me.fpsselect.value()) : 30;
  var mod = me.modselect ? Number(me.modselect.value()) : 1;
  var tolerance = ((Number($(ME).find('.rvs-sense').val())/55)-0.2) * -1;
  var factor = Number($(ME).find('.rvs-noise').val()) / 2;
  
  var d = {
    "start": start,
    "motion-detection": mot,
    "width": Number(res[0]),
    "height": Number(res[1]),
    "rotation": rot,
    "fps": fps,
    "modulus": mod,
    "motion-tolerance": tolerance,
    "noise-factor": factor
  };
  send_save(d, cb, me.peer);
};

me.ready = function(){
  var iss = $(ME).find('.if-start-sel');
  send_info(function(result){
    me.data = result.data ? result.data : {
      "start": false,
      "width": 640,
      "height": 480,
      "rotation": 0,
      "fps": 10,
      "motion-modulus": 1,
      "motion-detection": "false"
    };

    var b = me.data.start == 'true';
    $(ME).find('.dvrs-start').prop('checked', b);
    $(ME).find('.if-start').css('display', b ? 'block' : 'none');
    
    var w = me.data.width;
    var h = me.data.height;
    var r = w+'x'+h;
    
    var d = {
      "label": "Resolution",
      "list": [ "1920x1080", "1280x720", "800x600", "640x480", "480x360", "320x240", "240x180" ],
      "value": r
    };
    var el = $('<div class="shortselect"/>');
    $(ME).find('.if-start-sel').append(el);
    installControl(el[0], 'metabot', 'select', function(api){ me.resselect = api; }, d);

    var rot = me.data.rotation;
    d = {
      "label": "Rotation",
      "list": [ "0", "90", "180", "270" ],
      "value": rot
    };
    el = $('<div class="shortselect"/>');
    $(ME).find('.if-start-sel').append(el);
    installControl(el[0], 'metabot', 'select', function(api){ me.rotselect = api; }, d);
    
    var fps = me.data.fps;
    d = {
      "label": "Framerate",
      "list": [ "5", "10", "15", "20", "25", "30" ],
      "value": fps
    };
    el = $('<div class="shortselect"/>');
    $(ME).find('.if-start-sel').append(el);
    installControl(el[0], 'metabot', 'select', function(api){ me.fpsselect = api; }, d);

    var mod = me.data['motion-modulus'];
    d = {
      "label": "Samples per minute",
      "list": [ 
        { "id": "5", "name": "6" },
        { "id": "4", "name": "7.5" },
        { "id": "3", "name": "10" },
        { "id": "2", "name": "15" },
        { "id": "1", "name": "30" }
      ],
      "value": mod
    };
    el = $('<div class="longselect"/>');
    $(ME).find('.if-mot-start-sel').append(el);
    installControl(el[0], 'metabot', 'select', function(api){ me.modselect = api; }, d);

    b = me.data['motion-detection'] == 'true';
    $(ME).find('.dvrs-mot-start').prop('checked', b);
    $(ME).find('.if-mot-start').css('display', b ? 'block' : 'none');

    var mt = me.data['motion-tolerance'] ? me.data['motion-tolerance'] : 0;
    var nf = me.data['noise-factor'] ? me.data['noise-factor'] : 0;
    var sense = (0.2 - mt) * 55;
    var noise = nf * 2;
    $(ME).find('.rvs-sense').val(sense);
    $(ME).find('.rvs-noise').val(noise);
    
    document.body.api.ui.initSliders(ME);
  }, me.peer);
};

$(document).click(function(event) {
   window.lastElementClicked = event.target;
});

$(ME).find('.dvrs-start').change(function(){
  var b = $(this).prop('checked');
  $(ME).find('.if-start').css('display', b ? 'block' : 'none');
});

$(ME).find('.dvrs-mot-start').change(function(){
  var b = $(this).prop('checked');
  $(ME).find('.if-mot-start').css('display', b ? 'block' : 'none');
});