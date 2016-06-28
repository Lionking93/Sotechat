angular.module('chatProApp')
    .controller('adminController', ['$scope', '$http', '$uibModal','base64', 'adminService',
        function ($scope, $http, $uibModal, base64, adminService) {
            $scope.users = [];
            $scope.alerts = [];
            $scope.resetPsw = '';
            $scope.editPassword = '';
            $scope.adminView = 'admin/userHandling.tpl.html';
            $scope.newUser = {};

            var success = function(response) {
                if (response.data.status == 'OK') {
                    $scope.alerts.push({ type: 'success',dismiss: 4000,  msg: 'Toiminto onnistui!' })
                    getUsers();
                } else {
                    $scope.alerts.push({ type: 'danger', msg: 'Toiminto ei onnistunut! ' + response.data.error })
                }
            };

            $scope.toSettings = function() {
                $scope.adminView = 'admin/settings.tpl.html';
            };

            $scope.toUsers = function() {
                $scope.adminView = 'admin/userHandling.tpl.html';
            };
            
            $scope.createNewUser = function () {
                var user = '{"username": '+ $scope.newUser.Username + ', "loginName": '
                    + $scope.newUser.LoginName +', "password": '
                    + $scope.newUser.Password+ '}';
                adminService.createUser(base64.encode(user), function(response) {
                    if (response.data.status == 'OK') {
                        $scope.newUserBoolean = false;
                        $scope.newUser = {};
                    }
                    success(response);
                })
            };

            $scope.rpsw = function(userID) {
                $scope.resetPsw = userID;
            };

            $scope.doResetPsw = function(userID, newPsw) {
                adminService.resetPassword(userID, base64.encode(newPsw), success);
                $scope.resetPsw = '';
                $scope.editPassword = '';
            };

            $scope.removeUser = function(userID) {
                var modalInstance = $uibModal.open({
                    animation: true,
                    templateUrl: 'common/areUSureModal.tpl.html',
                    controller: 'AreUSureModalController'
                });

                modalInstance.result.then(function (result) {
                    adminService.delUser(userID, success);
                });
            };
            
            $scope.newUserBoolTrue = function() {
                $scope.newUserBoolean = true;
            }

            $scope.cancelEditOrReset = function() {
                $scope.newUserBoolean = false;
                $scope.resetPsw = '';
            };

            $scope.resetDatabase = function () {
                var modalInstance = $uibModal.open({
                    animation: true,
                    templateUrl: 'common/areUSureModal.tpl.html',
                    controller: 'AreUSureModalController'
                });

                modalInstance.result.then(function (result) {
                    adminService.resetDatabase(success);
                });
            };

            $scope.closeAlert = function(index) {
                $scope.alerts.splice(index, 1);
            };
            
            var getUsers = function() {
                $scope.users = [];
                adminService.getUsers(function(response) {
                    angular.forEach(response.data, function (key) {
                        $scope.users.push(key);
                    });
                });
            };

            getUsers();
        }]);