var monthnames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

var me = this;
var ME = $('#'+me.UUID)[0];

if (ME.DATA.peer){
  if (ME.DATA.peer != 'local') me.peer = ME.DATA.peer;
}
else me.peer = typeof CURRENTDEVICEID == 'undefined' ? null : CURRENTDEVICEID;

ME.DATA.peer = me.peer;
me.prefix = me.peer ? '../peerbot/remote/'+me.peer+'/' : '../';

me.ready = function(){
  document.body.api.ui.initSliders(ME);
  document.body.api.ui.initNavbar(ME);
  var el = $(ME).find('.mediagoeshere');
  ME.DATA.update = 3000;
  installControl(el[0], 'raspberrypi', 'cameramodule', function(api){
    me.CAM = api;
    el.append('<div class="overimage"><video class="rv-event-video hideme" controls="true" autoplay="true" allowfullscreen="true" width="100%" height="100%" style="background-color:black;"></video></div>');
  }, ME.DATA);
}

function updateSlide(){
  var year = $(ME).find('.dvryearselect').find('select').val();
  var month = $(ME).find('.dvrmonthselect').find('select').val();
  var date = $(ME).find('.dvrdateselect').find('select').val();
  var hour = $(ME).find('.dvrhourselect').find('select').val();
  var min = $(ME).find('.dvrminselect').find('select').val();
  var sec = $(ME).find('.dvrsecselect').find('select').val();

  var nudate = new Date(year, month, date, hour, Number(min), Number(sec), 0);
  me.dvrtime = nudate.getTime();
  buildTimePicker();
  
  var p = (me.dvrtime - me.CAM.info.first.time) / (me.CAM.info.last.time - me.CAM.info.first.time);
  $(ME).find('.datetimeslider').val(p*100).trigger("input");
}

function buildTimePicker(){
  var start = new Date(me.CAM.info.first.time);
  var stop = new Date(me.CAM.info.last.time);
  
  if (!me.dvrtime) me.dvrtime = me.CAM.info.last.time;
  else if (me.dvrtime < me.CAM.info.first.time) me.dvrtime = me.CAM.info.first.time;
  else if (me.dvrtime > me.CAM.info.last.time) me.dvrtime = me.CAM.info.last.time;
  var current = new Date(me.dvrtime);

  ME.DATA.update = false;
  me.CAM.loadSnap(me.dvrtime, function(){
    $(ME).find('.timestamp').css('color', 'white');
  }, 0, true);
  
  var startyear = start.getFullYear();
  var stopyear = stop.getFullYear();
  var currentyear = current.getFullYear();
  var years = [];
  for (var i=startyear; i<stopyear+1; i++) years.push(i);
  var d = {
    "list": years,
    "value": ""+currentyear,
    "label": "Year",
    "cb": updateSlide
  }
  var el = $(ME).find('.dvryearselect');
  installControl(el[0], 'metabot', 'select', function(api){}, d);
  
  var currentmonth = current.getMonth();
  var months = [];
  for (var i=0; i<12; i++){
    var mstart = new Date(currentyear, i, 1, 0, 0, 0, 0);
    var mend = new Date(currentyear, i+1, 1, 0, 0, 0, -1);
    var mst = mstart.getTime();
    var met = mend.getTime();
    if (met>me.CAM.info.first.time && mst<me.CAM.info.last.time){
      d = {
        "id": ""+i,
        "name": monthnames[i]
      };
      months.push(d);
    }
  }
  d = {
    "list": months,
    "value": ""+currentmonth,
    "label": "Month",
    "cb": updateSlide
  };
  el = $(ME).find('.dvrmonthselect');
  installControl(el[0], 'metabot', 'select', function(api){}, d);
  
  var currentdate = current.getDate();
  var sdate = mstart.getDate();
  var edate = mend.getDate()+1;
  var dates = [];
  for (var i=sdate;i<edate;i++){
    var dstart = new Date(currentyear, currentmonth, i, 0, 0, 0, 0);
    var dend = new Date(currentyear, currentmonth, i+1, 0, 0, 0, -1);
    var dst = dstart.getTime();
    var det = dend.getTime();
    if (det>me.CAM.info.first.time && dst<me.CAM.info.last.time) {
      dates.push(""+i);
    }
  }
  d = {
    "list": dates,
    "value": ""+currentdate,
    "label": "Date",
    "cb": updateSlide
  };
  el = $(ME).find('.dvrdateselect');
  installControl(el[0], 'metabot', 'select', function(api){}, d);
  
  var currenthour = current.getHours();
  var hours = [];
  for (var i=0; i<24; i++){
    var dstart = new Date(currentyear, currentmonth, currentdate, i, 0, 0, 0);
    var dend = new Date(currentyear, currentmonth, currentdate, i+1, 0, 0, -1);
    var dst = dstart.getTime();
    var det = dend.getTime();
    if (det>me.CAM.info.first.time && dst<me.CAM.info.last.time) {
      hours.push(""+i);
    }
  }
  d = {
    "list": hours,
    "value": ""+currenthour,
    "label": "Hour",
    "cb": updateSlide
  };
  el = $(ME).find('.dvrhourselect');
  installControl(el[0], 'metabot', 'select', function(api){}, d);
  
  var currentmin = current.getMinutes();
  var minutes = [];
  for (var i=0; i<60; i++){
    var dstart = new Date(currentyear, currentmonth, currentdate, currenthour, i, 0, 0);
    var dend = new Date(currentyear, currentmonth, currentdate, currenthour, i+1, 0, -1);
    var dst = dstart.getTime();
    var det = dend.getTime();
    if (det>me.CAM.info.first.time && dst<me.CAM.info.last.time) {
      minutes.push(""+padZero(i));
    }
  }
  d = {
    "list": minutes,
    "value": ""+padZero(currentmin),
    "label": "Minute",
    "cb": updateSlide
  };
  el = $(ME).find('.dvrminselect');
  installControl(el[0], 'metabot', 'select', function(api){}, d);
  
  var currentsec = current.getSeconds();
  var seconds = [];
  for (var i=0; i<60; i++){
    var dstart = new Date(currentyear, currentmonth, currentdate, currenthour, currentmin, i, 0);
    var dend = new Date(currentyear, currentmonth, currentdate, currenthour, currentmin, i+1, -1);
    var dst = dstart.getTime();
    var det = dend.getTime();
    if (det>me.CAM.info.first.time && dst<me.CAM.info.last.time) {
      seconds.push(""+padZero(i));
    }
  }
  d = {
    "list": seconds,
    "value": ""+padZero(currentsec),
    "label": "Second",
    "cb": updateSlide
  };
  el = $(ME).find('.dvrsecselect');
  installControl(el[0], 'metabot', 'select', function(api){}, d);
 
  buildThumbs();
};

function addAllFrames(el, list){
  if (list.length>0){
    var tstart = list.shift();
    var img = buildThumb(tstart, function(){
      addAllFrames(el, list);
    });
    img.data('timestamp', tstart);
    img.addClass('clickthumb');
    img.click(function(){
      $(ME).find('.clickthumb').css('border', 'thin solid black');
      $(this).css('border', 'thin solid #83bc00');
      me.CAM.loadSnap($(this).data('timestamp'), null, 0, true);
    });
    el.append(img).parent()[0].scrollLeft += 130;
  }
}

function buildThumbs(){
  var tstart = me.dvrtime;
  var dur = $(ME).find('.durationslider').val()*1000;
  var tstop = Math.min(tstart + dur, me.CAM.info.last.time)+1;
  el = $(ME).find('.thumbslider');
  el.empty();
  var list = [];
  while (tstart < tstop){
    list.push(tstart);
    tstart += 5000;
  }    
  if (me.addingevents) me.addingevents.length = 0;
  if (me.addingframes) me.addingframes.length = 0;
  me.addingframes = list;
  addAllFrames(el, list);
  setTimeout(function(){ addAllFrames(el, list); }, 1000);
  setTimeout(function(){ addAllFrames(el, list); }, 2000);
}

$(ME).find('.dvrtab').click(buildTimePicker);
$(ME).find('.livetab').click(function(){
  ME.DATA.update = 3000;
  $(ME).find('.timestamp').html('<i>loading...</i>').css('color', '#83bc00');
});

function addAllEvents(el, list){
  if (list.length>0){
    var d = list.pop();
    var url = null;
    if (d.list){
      for (var i in d.list){
        var frame = d.list[i];
        if (!frame.detected) break;
        if (frame.detected.length > 0){
          url = me.prefix+'raspberrypi/'+frame.img;
          break;
        }
      }
    }
    var event = buildThumb(d.time, function(){
      addAllEvents(el, list);
    }, url);
    var secs = Math.floor((d.list[d.list.length-1].time - d.time)/1000);
    var mins = Math.floor(secs/60);
    secs = padZero(secs % 60);

    event.data('event', d);
    event.addClass('clickevent');
    el.prepend(event).parent()[0].scrollLeft += 130;
    event.click(function(){
      $(ME).find('.clickevent').css('border', 'thin solid black');
      $(this).css('border', 'thin solid #83bc00');
      var d = $(this).data('event');
      me.dvrtime = d.time;
      buildTimePicker();
      var dur = Math.floor((d.list[d.list.length-1].time - d.time)/1000);
      console.log(dur);
      $(ME).find('.durationslider').val(dur).trigger("input");
      $(ME).find('.dvrplaybutton').click();
    });

    var el5 = '<div class="thumbduration">'+mins+":"+secs+'</div>';
    event.append(el5);
  }
}

$(ME).find('.eventstab').click(function(){
  device_events(me.CAM.info.first.time, me.CAM.info.last.time, function(result){
    var el = $(ME).find('.eventslider');
    el.empty();
    
    if (me.addingevents) me.addingevents.length = 0;
    me.addingevents = result.data.list;
    
    addAllEvents(el, result.data.list);
    setTimeout(function(){ addAllEvents(el, result.data.list); }, 1000);
    setTimeout(function(){ addAllEvents(el, result.data.list); }, 2000);
  });
});

$(ME).find('.datetimeslider').change(function(){
  var p = $(this).val()/100;
  me.dvrtime = Math.floor(me.CAM.info.first.time + (p * (me.CAM.info.last.time - me.CAM.info.first.time)));
  buildTimePicker();
});

$(ME).find('.durationslider').change(function(){
  var secs = $(this).val();
  var mins = Math.floor(secs/60);
  secs = padZero(secs % 60);
  $(ME).find('.durationslidervalue').text(mins+":"+secs);
  buildThumbs();
});

function parseTime(d){
  return parseDate(d)+':'+padZero(d.getSeconds());
}

function buildThumb(millis, cb, img){
  var url = img ? img : me.CAM.previewPath(millis);
  var time = parseTime(new Date(millis));
  console.log(time);
  var el1 = $('<div style="position:relative;display:inline-block;border:thin solid black;"></div>');
  var el2 = $('<img width="128px" height="96px" src="../botmanager/asset/raspberrypi/loading_trippy.gif">');
  var el4 = '<div class="thumbtimestamp">'+time+'</div>';
  el1.append(el2);
  el1.append(el4);
  
    
  // FIXME - Choking on relayed connections-- disabled websocket image load for now
  // FIXME - Doesn't work without websocket
  //if (false) { 
  //if (SOCK && SOCK.readyState == SOCK.OPEN){
  if (SOCK && SOCK.readyState == SOCK.OPEN && document.peers && document.peers[me.peer] && document.peers[me.peer].tcp) {
    json(me.prefix+"metabot/call","db=raspberrypi&name=cameramodule&cmd=jpeg&args={%22time%22:"+millis+"}", function(result){ 
      var stream = result.msg;
      tempfile(me.peer, stream, function(result){
        el2.prop('src', 'data:image/jpg;base64,'+result.data);
      });
      if (cb) cb();
    });
  }
  else {
    var el3 = $('<img style="display:none;">');
    el3.load(function(){
      el2.prop('src', url);
      if (cb) cb();
    });
    el3.error(function(){
      console.log("COULDN'T LOAD "+url);
      el2.prop('src', '../botmanager/asset/raspberrypi/nosignal.jpg');
      if (cb) cb();
    });
    el3.prop('src', url);
    el1.append(el3);    
  }
  return el1;
}

$(ME).find('.dvrplaybutton').click(function(){
  var el = $(ME).find('.live')[0];
  $(el).prop("src", "../botmanager/asset/raspberrypi/processing.gif");
  
  var start = me.dvrtime;
  var dur = $(ME).find('.durationslider').val()*1000;
  transcode(start, dur, function(url){
    var vid = $(ME).find("video")[0];
    $(vid).prop('poster', me.CAM.previewPath(start));
    $(vid).parent().css('display', 'block');
    vid.src = url;
    vid.play();
  });
});

$(ME).find('.dvrdownloadbutton').click(function(){
  var el = $(ME).find('.live')[0];
  var oldsrc = $(el).prop("src");
  $(el).prop("src", "../botmanager/asset/raspberrypi/processing.gif");
  
  var start = me.dvrtime;
  var dur = $(ME).find('.durationslider').val()*1000;
  transcode(start, dur, function(url){
    var name = url.substring(url.lastIndexOf('/')+1);
    var clickme = $('<a class="overimage" download="'+name+'"><img src="../botmanager/asset/raspberrypi/download.png"></a>');
    clickme.css('display', 'block');
    clickme.prop('href', url);
    clickme.click(function(){
      $(el).prop("src", oldsrc);
      $('.overimage').css('display', 'none');
      buildTimePicker();
    });
    $(ME).find('.mediagoeshere').append(clickme);
  });
});

function transcode(start, dur, cb){
  var stop = start + dur;
  var format = 'mp4';
  device_build(start, stop, format, function(result){
    var src = me.prefix+'raspberrypi/generated/mp4/'+result.data.name;
    cb(src);
  });
}

function device_build(start, stop, format, cb){
  var args = {start: start, stop: stop, format: format};
  args = encodeURIComponent(JSON.stringify(args));
  json(me.prefix+'metabot/call', 'db=raspberrypi&name=cameramodule&cmd=build&args='+args, function(result){
    cb(result);
  });
}

function device_events(start, stop, cb){
  var args = {start: start, stop: stop};
  args = encodeURIComponent(JSON.stringify(args));
  $.getJSON(me.prefix+'metabot/call?db=raspberrypi&name=cameramodule&cmd=events&args='+args, function(result){
    cb(result);
  });
}





