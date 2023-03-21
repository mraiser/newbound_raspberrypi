var me = this;
var ME = $('#'+me.UUID)[0];

me.ready = function(){
  componentHandler.upgradeAllRegistered();
};

function resize(){
  var h = $(ME).height();
  var w = $(ME).width();
  var m = Math.min(h,w);
  var l = (w - m) / 2;
  $(ME).find('.pimodel').height(m).width(m).css('left',l+'px');
}

$(window).resize(resize);
