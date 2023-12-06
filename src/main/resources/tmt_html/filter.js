$(document).ready(function() {
    $("#checkbox_info").prop("checked", true);
    $("#checkbox_warn").prop("checked", true);
    $("#checkbox_fail").prop("checked", true);
    $("#checkbox_error").prop("checked", true);
    $(".filter_checkbox").change(function() {
        $("table").hide();
        if (this.checked) {
            $("." + $(this).val()).show();
        } else {
            $("." + $(this).val()).hide();
        }
        $("table").show();
    }).change();
});
