$(document).ready(function() {
    $(".filter_checkbox").change(function() {
        if (this.checked) {
            $("." + $(this).val()).show();
        }
        else {
            $("." + $(this).val()).hide();
        }
    }).change();
    $("#checkbox_fail").prop("checked", true);
    $("#checkbox_error").prop("checked", true);
});
